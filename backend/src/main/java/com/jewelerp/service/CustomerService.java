package com.jewelerp.service;

import com.jewelerp.config.ApiException;
import com.jewelerp.dto.Requests;
import com.jewelerp.model.AppUser;
import com.jewelerp.model.Customer;
import com.jewelerp.model.Invoice;
import com.jewelerp.model.LoyaltyTransaction;
import com.jewelerp.model.OldGoldPurchase;
import com.jewelerp.repository.CustomerRepository;
import com.jewelerp.repository.InvoiceRepository;
import com.jewelerp.repository.LoyaltyTransactionRepository;
import com.jewelerp.repository.OldGoldPurchaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customers;
    private final InvoiceRepository invoices;
    private final OldGoldPurchaseRepository oldGold;
    private final LoyaltyTransactionRepository loyalty;
    private final AuditService audit;

    public record CustomerRow(Customer customer, int invoiceCount, BigDecimal totalSpent) {
    }

    public record Detail(Customer customer, List<Invoice> invoices, List<OldGoldPurchase> oldGold,
                         List<LoyaltyTransaction> loyalty) {
    }

    public record UpcomingEvent(Customer customer, String type, LocalDate nextDate, long daysUntil) {
    }

    public List<CustomerRow> list(String q, String tier) {
        return customers.search(q == null ? "" : q.trim(), tier == null ? "" : tier.trim()).stream().map(c -> {
            List<Invoice> billed = invoices.findByCustomerIdOrderByBilledAtDesc(c.getId()).stream()
                    .filter(i -> !"cancelled".equals(i.getStatus()) && !"estimate".equals(i.getStatus())).toList();
            BigDecimal spent = billed.stream().map(Invoice::getGrandTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
            return new CustomerRow(c, billed.size(), spent);
        }).toList();
    }

    public Detail detail(Long id) {
        Customer c = customers.findById(id).orElseThrow(() -> ApiException.notFound("Customer"));
        return new Detail(c, invoices.findByCustomerIdOrderByBilledAtDesc(id),
                oldGold.findByCustomerIdOrderByIdDesc(id), loyalty.findByCustomerIdOrderByIdDesc(id));
    }

    @Transactional
    public Customer create(Requests.CreateCustomer req, AppUser user) {
        String phone = req.phone().trim();
        if (customers.existsByPhone(phone)) {
            throw new ApiException(HttpStatus.CONFLICT, "A customer with phone " + phone + " already exists");
        }
        Customer c = new Customer();
        c.setCustomerCode(String.format("CUST-%04d", customers.maxId() + 1));
        c.setName(req.name().trim());
        c.setPhone(phone);
        c.setEmail(req.email());
        c.setAddress(req.address());
        c.setDob(req.dob());
        c.setAnniversary(req.anniversary());
        c.setPanNumber(req.panNumber());
        c.setGroupTier(req.groupTier());
        c.setCreditLimit(req.creditLimit() == null ? BigDecimal.ZERO : req.creditLimit());
        c.setPreferences(req.preferences());
        customers.save(c);
        audit.log(user, "CRM", "create", "Registered customer " + c.getName() + " (" + c.getCustomerCode() + ")");
        return c;
    }

    @Transactional
    public Customer adjustLoyalty(Long id, Requests.Loyalty req, AppUser user) {
        Customer c = customers.findById(id).orElseThrow(() -> ApiException.notFound("Customer"));
        if ("redeem".equals(req.type())) {
            if (c.getLoyaltyPoints() < req.points()) {
                throw ApiException.badRequest("Insufficient loyalty points");
            }
            c.setLoyaltyPoints(c.getLoyaltyPoints() - req.points());
        } else {
            c.setLoyaltyPoints(c.getLoyaltyPoints() + req.points());
        }
        LoyaltyTransaction t = new LoyaltyTransaction();
        t.setCustomer(c);
        t.setPoints(req.points());
        t.setType(req.type());
        t.setDescription(req.description());
        loyalty.save(t);
        audit.log(user, "CRM", "loyalty", req.type() + " " + req.points() + " points for " + c.getName());
        return c;
    }

    @Transactional
    public void delete(Long id, AppUser user) {
        Customer c = customers.findById(id).orElseThrow(() -> ApiException.notFound("Customer"));
        if (!invoices.findByCustomerIdOrderByBilledAtDesc(id).isEmpty()) {
            throw ApiException.badRequest("This customer has invoices and cannot be deleted");
        }
        loyalty.deleteAll(loyalty.findByCustomerIdOrderByIdDesc(id));
        customers.delete(c);
        audit.log(user, "CRM", "delete", "Deleted customer " + c.getName());
    }

    public List<UpcomingEvent> upcomingEvents(int windowDays) {
        LocalDate today = LocalDate.now();
        List<UpcomingEvent> events = new ArrayList<>();
        for (Customer c : customers.findAll()) {
            addIfUpcoming(events, c, "birthday", c.getDob(), today, windowDays);
            addIfUpcoming(events, c, "anniversary", c.getAnniversary(), today, windowDays);
        }
        events.sort(Comparator.comparingLong(UpcomingEvent::daysUntil));
        return events;
    }

    private static void addIfUpcoming(List<UpcomingEvent> out, Customer c, String type, LocalDate date,
                                      LocalDate today, int windowDays) {
        if (date == null) {
            return;
        }
        LocalDate next = nextOccurrence(date, today.getYear());
        if (next.isBefore(today)) {
            next = nextOccurrence(date, today.getYear() + 1);
        }
        long days = ChronoUnit.DAYS.between(today, next);
        if (days <= windowDays) {
            out.add(new UpcomingEvent(c, type, next, days));
        }
    }

    private static LocalDate nextOccurrence(LocalDate date, int year) {
        if (date.getMonthValue() == 2 && date.getDayOfMonth() == 29 && !java.time.Year.isLeap(year)) {
            return LocalDate.of(year, 2, 28);
        }
        return date.withYear(year);
    }
}
