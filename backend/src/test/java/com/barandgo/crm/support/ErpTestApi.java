package com.barandgo.crm.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.barandgo.crm.identity.User;
import com.barandgo.crm.identity.UserRole;
import com.barandgo.crm.identity.internal.TokenService;
import com.barandgo.crm.identity.internal.UserRepository;
import com.jayway.jsonpath.JsonPath;

/**
 * Preparación de datos para los tests de integración del ERP, siempre a través de la API: cada test
 * crea lo que necesita y los tests no dependen de datos sembrados ni unos de otros.
 */
public class ErpTestApi {

    private final MockMvc mockMvc;
    private final UserRepository userRepository;
    private final TokenService tokenService;

    public ErpTestApi(MockMvc mockMvc, UserRepository userRepository, TokenService tokenService) {
        this.mockMvc = mockMvc;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
    }

    /** Usuario nuevo con ese rol, guardado en BD, y su cabecera Authorization. */
    public String bearer(UserRole role) {
        String email = role.name().toLowerCase() + "-" + System.nanoTime() + "@bar.com";
        User user = new User();
        user.setName("Usuario " + role);
        user.setEmail(email);
        user.setPassword("no-se-usa");
        user.setPhone("+34600000099");
        user.setRole(role);
        User saved = userRepository.save(user);
        return "Bearer " + tokenService.generateToken(saved.getId(), email, role);
    }

    public ResultActions call(MockHttpServletRequestBuilder request, String token, String body) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body == null ? "" : body));
    }

    public ResultActions fetch(String url, String token) throws Exception {
        return mockMvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, token));
    }

    public static long idOf(ResultActions result) throws Exception {
        Number id = JsonPath.read(body(result), "$.id");
        return id.longValue();
    }

    public static String body(ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString();
    }

    public long itemCategory(String token, String name) throws Exception {
        return idOf(call(post("/api/item-categories"), token, "{\"name\":\"" + name + "\"}")
                .andExpect(status().isCreated()));
    }

    public long supplier(String token, String taxId, String name, int leadTimeDays) throws Exception {
        return idOf(call(post("/api/suppliers"), token, """
                {"taxId":"%s","legalName":"%s","leadTimeDays":%d}
                """.formatted(taxId, name, leadTimeDays)).andExpect(status().isCreated()));
    }

    public long vatRateId(String token, String name) throws Exception {
        String rates = body(fetch("/api/vat-rates", token).andExpect(status().isOk()));
        List<Number> ids = JsonPath.read(rates, "$[?(@.name == '" + name + "')].id");
        return ids.get(0).longValue();
    }

    public long warehouse(String token, String name, String type) throws Exception {
        return idOf(call(post("/api/warehouses"), token, """
                {"name":"%s","type":"%s"}
                """.formatted(name, type)).andExpect(status().isCreated()));
    }

    /** Artículo a partir de un JSON completo (ver StockItemDTO). */
    public long stockItem(String token, String json) throws Exception {
        return idOf(call(post("/api/stock-items"), token, json).andExpect(status().isCreated()));
    }

    public void tariff(String token, long itemId, long supplierId, String unit, String price) throws Exception {
        call(put("/api/stock-items/" + itemId + "/suppliers/" + supplierId), token, """
                {"purchaseUnit":"%s","unitPrice":%s}
                """.formatted(unit, price)).andExpect(status().isOk());
    }

    /** Pedido creado y enviado; linesJson es el array de líneas. */
    public long sentPurchaseOrder(String token, long supplierId, String linesJson) throws Exception {
        long orderId = idOf(call(post("/api/purchase-orders"), token,
                "{\"supplierId\":" + supplierId + ",\"lines\":" + linesJson + "}").andExpect(status().isCreated()));
        call(post("/api/purchase-orders/" + orderId + "/send"), token, null).andExpect(status().isOk());
        return orderId;
    }
}
