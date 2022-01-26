package allanlewis.spring

import allanlewis.api.RestApi
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension

@SpringBootTest
@ExtendWith(SpringExtension::class)
class ApplicationTest {

    @Autowired
    private lateinit var restApi: RestApi

    @Test
    @DisplayName("REST API exists")
    fun restApiExists() {
        Assertions.assertNotNull(restApi)
    }

}