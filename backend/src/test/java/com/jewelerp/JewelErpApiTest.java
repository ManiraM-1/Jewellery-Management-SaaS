package com.jewelerp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JewelErpApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    private String login(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", "password"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(body).get("token").asText();
    }

    private JsonNode getJson(String url, String token) throws Exception {
        String body = mvc.perform(get(url).header("Authorization", token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    @Test
    void rejectsWrongPasswordAndMissingToken() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@jewelerp.in\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/dashboard")).andExpect(status().isUnauthorized());
    }

    @Test
    void dashboardShowsSeededData() throws Exception {
        JsonNode dash = getJson("/api/dashboard", login("admin@jewelerp.in"));
        assertThat(dash.get("stockPieces").asInt()).isGreaterThan(0);
        assertThat(dash.get("rates").get("Gold 24K").decimalValue()).isPositive();
        assertThat(dash.get("recentInvoices").size()).isGreaterThan(0);
    }

    @Test
    void cashierBillsAPieceAndItIsMarkedSold() throws Exception {
        String token = login("cashier@jewelerp.in");
        JsonNode catalog = getJson("/api/pos/catalog", token);
        JsonNode piece = catalog.get(0);

        Map<String, Object> line = Map.ofEntries(
                Map.entry("inventoryItemId", piece.get("id").asLong()),
                Map.entry("metalRate", piece.get("metalRate").decimalValue()),
                Map.entry("wastagePct", piece.get("wastagePct").decimalValue()),
                Map.entry("makingChargeType", piece.get("makingChargeType").asText()),
                Map.entry("makingChargeRate", piece.get("makingChargeRate").decimalValue()),
                Map.entry("stoneCharge", piece.get("stoneCharge").decimalValue()),
                Map.entry("diamondCharge", piece.get("diamondCharge").decimalValue()),
                Map.entry("hallmarkCharge", piece.get("hallmarkCharge").decimalValue()),
                Map.entry("otherCharge", piece.get("otherCharge").decimalValue()),
                Map.entry("gstRate", piece.get("gstRate").decimalValue()));
        Map<String, Object> checkout = Map.of(
                "branchId", piece.get("branchId").asLong(),
                "items", java.util.List.of(line),
                "gstType", "intra",
                "paymentMethod", "cash",
                "paidAmount", 0);

        String body = mvc.perform(post("/api/pos/checkout").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(checkout)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode invoice = json.readTree(body);

        assertThat(invoice.get("status").asText()).isEqualTo("credit");
        assertThat(invoice.get("cgstAmount").decimalValue()).isEqualByComparingTo(invoice.get("sgstAmount").decimalValue());
        assertThat(invoice.get("items").get(0).get("netWeight").decimalValue())
                .isEqualByComparingTo(piece.get("netWeight").decimalValue());

        JsonNode after = getJson("/api/pos/catalog", token);
        for (JsonNode i : after) {
            assertThat(i.get("id").asLong()).isNotEqualTo(piece.get("id").asLong());
        }

        mvc.perform(post("/api/invoices/" + invoice.get("id").asLong() + "/cancel").header("Authorization", token))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/invoices/" + invoice.get("id").asLong() + "/cancel")
                        .header("Authorization", login("manager@jewelerp.in")))
                .andExpect(status().isOk());
    }

    @Test
    void artisanCannotOpenPos() throws Exception {
        mvc.perform(get("/api/pos/catalog").header("Authorization", login("rahim@jewelerp.in")))
                .andExpect(status().isForbidden());
    }
}
