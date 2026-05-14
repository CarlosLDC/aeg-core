package com.aeg.core;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aeg.core.company.Company;
import com.aeg.core.company.CompanyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false) // disable security filters for test
@ActiveProfiles("test")
@Transactional
public class BranchCreationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testCreateBranch() throws Exception {
        Company c = new Company();
        c.setBusinessName("Test Company");
        c.setRif("J-12345678");
        c.setContributorType(com.aeg.core.company.ContributorType.ORDINARIO);
        c = companyRepository.save(c);

        String json = """
            {
              "companyId": %d,
              "city": "Caracas",
              "state": "Distrito Capital",
              "address": "Av. Principal de Los Ruices",
              "phone": "+58 212 555 0101",
              "email": "sucursal.caracas@aeg.local",
              "isClient": true,
              "isDistributor": true,
              "isServiceCenter": true
            }
            """.formatted(c.getId());

        mockMvc.perform(post("/api/branches")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isCreated());
    }
}
