package com.jewelerp.seed;

import com.jewelerp.dto.Requests;
import com.jewelerp.model.AppUser;
import com.jewelerp.model.Branch;
import com.jewelerp.model.Category;
import com.jewelerp.model.Counter;
import com.jewelerp.model.Customer;
import com.jewelerp.model.InventoryItem;
import com.jewelerp.model.MetalRate;
import com.jewelerp.model.Product;
import com.jewelerp.model.Role;
import com.jewelerp.model.StockMovement;
import com.jewelerp.pricing.JewelleryPricing;
import com.jewelerp.repository.BranchRepository;
import com.jewelerp.repository.CategoryRepository;
import com.jewelerp.repository.CounterRepository;
import com.jewelerp.repository.CustomerRepository;
import com.jewelerp.repository.InventoryItemRepository;
import com.jewelerp.repository.MetalRateRepository;
import com.jewelerp.repository.ProductRepository;
import com.jewelerp.repository.StockMovementRepository;
import com.jewelerp.repository.UserRepository;
import com.jewelerp.service.OldGoldService;
import com.jewelerp.service.PosService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {

    private final BranchRepository branches;
    private final CounterRepository counters;
    private final UserRepository users;
    private final MetalRateRepository rates;
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final InventoryItemRepository items;
    private final StockMovementRepository movements;
    private final CustomerRepository customers;
    private final PasswordEncoder passwordEncoder;
    private final PosService pos;
    private final OldGoldService oldGold;

    @Value("${app.seed-demo-data}")
    private boolean enabled;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled || users.count() > 0) {
            return;
        }

        Branch hq = branch("Zaveri Bazaar Flagship", "MUM-ZB", "Mumbai", "Maharashtra", true);
        Branch bandra = branch("Bandra West Boutique", "MUM-BW", "Mumbai", "Maharashtra", false);
        Counter cGold = counter(hq, "Gold Bridal Counter", "ZB-G1");
        Counter cDia = counter(hq, "Solitaire Lounge", "ZB-D1");
        Counter cSilver = counter(hq, "Silver & Coins", "ZB-S1");
        Counter cBandra = counter(bandra, "Boutique Main", "BW-M1");

        AppUser admin = user(hq, "Rajesh Zaveri", "admin@jewelerp.in", Role.ADMIN, "EMP-001");
        user(hq, "Meera Deshmukh", "manager@jewelerp.in", Role.MANAGER, "EMP-002");
        AppUser cashier = user(hq, "Suresh Kulkarni", "cashier@jewelerp.in", Role.CASHIER, "EMP-003");
        AppUser ananya = user(hq, "Ananya Sharma", "ananya@jewelerp.in", Role.SALESPERSON, "EMP-004");
        user(hq, "Karigar Rahim Bhai", "rahim@jewelerp.in", Role.ARTISAN, "EMP-005");
        user(bandra, "Vikram Kapoor", "vikram@jewelerp.in", Role.MANAGER, "EMP-006");

        BigDecimal base24 = new BigDecimal("7850");
        for (int d = 6; d >= 0; d--) {
            BigDecimal g24 = base24.add(BigDecimal.valueOf((6 - d) * 25L - 75));
            LocalDateTime eff = LocalDate.now().minusDays(d).atTime(9, 30);
            for (String purity : List.of("24K", "22K", "20K", "18K", "14K", "9K")) {
                rate("Gold", purity, JewelleryPricing.deriveRateFrom24K(g24, purity), eff, admin);
            }
            rate("Silver", "999", BigDecimal.valueOf(95 + (d % 3)), eff, admin);
            rate("Silver", "925", BigDecimal.valueOf(88 + (d % 3)), eff, admin);
            rate("Platinum", "950", new BigDecimal("3150"), eff, admin);
        }

        Category rings = category("Rings", "RNG", "per_gram", "650", "2.0");
        Category necklaces = category("Necklaces", "NCK", "percentage", "12.0", "3.0");
        Category bangles = category("Bangles", "BNG", "per_gram", "550", "2.5");
        Category earrings = category("Earrings", "EAR", "per_gram", "700", "2.0");
        Category coins = category("Coins & Bars", "COIN", "fixed", "250", "0");

        Object[][] stock = {
                {"SKU-RNG-001", "Maharani Kundan Gold Ring", rings, "Ring", "Gold", "22K", "per_gram", "650", "2.0", "12.450", "1.200", "0", "KR2201", "1500", hq, cGold},
                {"SKU-NCK-001", "Royal Temple Bridal Necklace", necklaces, "Necklace", "Gold", "22K", "percentage", "12.0", "3.0", "48.800", "3.500", "0", "TN2202", "4500", hq, cGold},
                {"SKU-BNG-001", "Patla Traditional Gold Bangles (Pair)", bangles, "Bangles", "Gold", "22K", "per_gram", "550", "2.0", "32.100", "0", "0", "PB2203", "0", hq, cGold},
                {"SKU-RNG-002", "Forever Solitaire Diamond Ring", rings, "Ring", "Gold", "18K", "fixed", "4500", "1.0", "5.200", "0.150", "0.750", "SD1804", "0", hq, cDia},
                {"SKU-EAR-001", "Jhumka Heritage Gold Earrings", earrings, "Earrings", "Gold", "22K", "per_gram", "700", "2.5", "14.600", "0.400", "0", "JH2205", "800", bandra, cBandra},
                {"SKU-COIN-001", "Lakshmi 24K Gold Coin 10g", coins, "Coins", "Gold", "24K", "fixed", "300", "0", "10.000", "0", "0", "LC2406", "0", hq, cSilver},
                {"SKU-SLV-001", "Pure Silver Pooja Thali Set", coins, "Coins", "Silver", "999", "per_gram", "35", "0", "250.000", "0", "0", null, "0", hq, cSilver},
                {"SKU-CHN-001", "Rope Design Gold Chain 20in", necklaces, "Chain", "Gold", "22K", "per_gram", "450", "1.5", "18.250", "0", "0", "RC2208", "0", hq, cGold},
        };
        List<InventoryItem> created = new ArrayList<>();
        int n = 1;
        for (Object[] s : stock) {
            Product p = new Product();
            p.setSku((String) s[0]);
            p.setName((String) s[1]);
            p.setCategory((Category) s[2]);
            p.setProductType((String) s[3]);
            p.setMetalType((String) s[4]);
            p.setPurity((String) s[5]);
            p.setMakingChargeType((String) s[6]);
            p.setMakingChargeValue(new BigDecimal((String) s[7]));
            p.setWastagePct(new BigDecimal((String) s[8]));
            p.setDescription("Crafted " + s[5] + " " + s[4] + " " + s[3]);
            products.save(p);

            BigDecimal gross = new BigDecimal((String) s[9]);
            BigDecimal stone = new BigDecimal((String) s[10]);
            BigDecimal diamond = new BigDecimal((String) s[11]);
            InventoryItem item = new InventoryItem();
            item.setItemCode(String.format("JW-%05d", n));
            item.setBarcode(String.format("8901000%05d", n));
            item.setProduct(p);
            item.setBranch((Branch) s[14]);
            item.setCounter((Counter) s[15]);
            item.setGrossWeight(gross);
            item.setStoneWeight(stone);
            item.setDiamondWeight(diamond);
            item.setNetWeight(gross.subtract(stone));
            item.setPurity((String) s[5]);
            item.setHuid((String) s[12]);
            item.setHallmarkCenter(s[12] == null ? null : "BIS Mumbai Assaying Center");
            item.setHallmarkCharge(s[12] == null ? BigDecimal.ZERO : new BigDecimal("45"));
            item.setPurchaseCost(JewelleryPricing.r2(gross.subtract(stone).multiply(new BigDecimal("6800"))));
            item.setStoneCharge(new BigDecimal((String) s[13]));
            item.setDiamondCharge(JewelleryPricing.r2(diamond.multiply(new BigDecimal("75000"))));
            items.save(item);

            StockMovement m = new StockMovement();
            m.setInventoryItem(item);
            m.setBranch(item.getBranch());
            m.setType("stock_in");
            m.setReference("OPENING-STOCK");
            m.setNotes("Initial showroom stock registration");
            m.setUser(admin);
            movements.save(m);
            created.add(item);
            n++;
        }

        Customer priya = customer("Priya Singhania", "9820112233", "vip", LocalDate.of(1988, 11, 18),
                LocalDate.of(2015, 12, 5), "Prefers 22K antique temple jewellery");
        customer("Rohan Mehta", "9820445566", "gold", LocalDate.of(1992, 5, 14), null, null);
        customer("Lakshmi Narayanan", "9840011122", "silver", LocalDate.now().plusDays(3).withYear(1979), null,
                "Buys gold coins every Akshaya Tritiya");

        oldGold.create(new Requests.OldGold(hq.getId(), priya.getId(), "Old 22K chain, broken clasp",
                new BigDecimal("8.500"), BigDecimal.ZERO, "22K", new BigDecimal("91.60"),
                pos.availableItems().stream().filter(i -> "24K".equals(i.purity())).findFirst()
                        .map(PosService.CatalogItem::metalRate).orElse(base24),
                new BigDecimal("2.00"), new BigDecimal("1.00"), "exchange", true), admin);

        PosService.CatalogItem ring = pos.availableItems().stream()
                .filter(i -> i.id().equals(created.get(0).getId())).findFirst().orElseThrow();
        Requests.PosItem line = new Requests.PosItem(ring.id(), null, null, null, null, null, null, null,
                ring.metalRate(), ring.wastagePct(), null, ring.makingChargeType(), ring.makingChargeRate(),
                ring.stoneCharge(), ring.diamondCharge(), ring.hallmarkCharge(), ring.otherCharge(), BigDecimal.ZERO,
                ring.gstRate());
        pos.checkout(new Requests.Checkout(hq.getId(), priya.getId(), ananya.getId(), "sale", List.of(line),
                BigDecimal.ZERO, "intra", "nearest_rupee", null, null, null, "upi", "UPI-DEMO-001",
                new BigDecimal("50000"), "Demo invoice"), cashier);
    }

    private Branch branch(String name, String code, String city, String state, boolean headOffice) {
        Branch b = new Branch();
        b.setName(name);
        b.setCode(code);
        b.setCity(city);
        b.setState(state);
        b.setGstin("27AABCS1429B1Z5");
        b.setHeadOffice(headOffice);
        return branches.save(b);
    }

    private Counter counter(Branch branch, String name, String code) {
        Counter c = new Counter();
        c.setBranch(branch);
        c.setName(name);
        c.setCode(code);
        return counters.save(c);
    }

    private AppUser user(Branch branch, String name, String email, String role, String code) {
        AppUser u = new AppUser();
        u.setBranch(branch);
        u.setName(name);
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode("password"));
        u.setRole(role);
        u.setEmployeeCode(code);
        return users.save(u);
    }

    private void rate(String metal, String purity, BigDecimal value, LocalDateTime eff, AppUser admin) {
        MetalRate r = new MetalRate();
        r.setMetalType(metal);
        r.setPurity(purity);
        r.setRatePerGram(value);
        r.setEffectiveAt(eff);
        r.setStatus("approved");
        r.setCreatedBy(admin);
        r.setApprovedBy(admin);
        rates.save(r);
    }

    private Category category(String name, String code, String makingType, String makingValue, String wastage) {
        Category c = new Category();
        c.setName(name);
        c.setCode(code);
        c.setDefaultMakingChargeType(makingType);
        c.setDefaultMakingChargeValue(new BigDecimal(makingValue));
        c.setDefaultWastagePct(new BigDecimal(wastage));
        return categories.save(c);
    }

    private Customer customer(String name, String phone, String tier, LocalDate dob, LocalDate anniversary,
                              String preferences) {
        Customer c = new Customer();
        c.setCustomerCode(String.format("CUST-%04d", customers.count() + 1));
        c.setName(name);
        c.setPhone(phone);
        c.setGroupTier(tier);
        c.setDob(dob);
        c.setAnniversary(anniversary);
        c.setPreferences(preferences);
        return customers.save(c);
    }
}
