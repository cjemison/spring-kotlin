package com.kotlin.springkotlin

import com.fasterxml.jackson.annotation.JsonProperty
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter
import java.sql.Timestamp
import java.util.*
import javax.persistence.*

fun dateTime(): DateTime = DateTime.now(DateTimeZone.UTC)

fun epoch(): Long = dateTime().millis

fun timestamp(): Timestamp = Timestamp(epoch())

fun randString(): String = UUID.randomUUID().toString()

@Configuration
@EnableWebMvc
@EnableJpaRepositories(basePackages = ["com.kotlin.springkotlin"])
@EnableTransactionManagement
class WebConfig : WebMvcConfigurerAdapter()

@MappedSuperclass
sealed class BaseEO(
        @Id
        @Column(name = "id", nullable = false) open var id: String = randString(),
        @Column(name = "active", nullable = false) open var active: Boolean = false,
        @Column(name = "created_date", nullable = false) open var createdDate: Timestamp = timestamp(),
        @Column(name = "created_epoch", nullable = false) open var epoch: Long = epoch()
)

@Entity
@Table(name = "app_table")
data class AppEO(
        @Column(name = "app_name", nullable = false) var appName: String = UUID.randomUUID().toString()
) : BaseEO()

@Repository
interface AppEORepository : CrudRepository<AppEO, String> {

    @Query("Select a from AppEO a where a.appName like trim(:appName)")
    fun findByAppName(@Param("appName") appName: String): Iterable<AppEO>
}


sealed class BaseDO
data class AppDO(@JsonProperty("id")
                 val id: String,
                 @JsonProperty("application_name")
                 val appName: String,
                 @JsonProperty("epoch")
                 val epoch: Long) : BaseDO()

fun AppEO.toDomain(): AppDO = AppDO(id = this.id, appName = this.appName, epoch = this.epoch)


@RestController
class AppController @Autowired constructor(val appEORepository: AppEORepository) {

    @RequestMapping(value = ["/v1/app"], method = [RequestMethod.GET],
            produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun get(): ResponseEntity<Iterable<BaseDO>> {
        appEORepository.save(AppEO("Cornelius"))
        val list: List<AppDO> = appEORepository
                .findAll()
                .map { appEO -> appEO.toDomain() }
        return ResponseEntity.ok(list)
    }

    @RequestMapping(value = ["/v1/app/{id}"], method = [RequestMethod.GET],
            produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun getById(@PathVariable("id") id: String): ResponseEntity<BaseDO> {

        return ResponseEntity.ok(appEORepository.findById(id).get().toDomain())
    }

    @RequestMapping(value = ["/v1/app/name/{name}"], method = [RequestMethod.GET],
            produces = [MediaType.APPLICATION_JSON_UTF8_VALUE])
    fun getByName(@PathVariable("name") name: String): ResponseEntity<Iterable<BaseDO>> {

        val list: List<AppDO> = appEORepository
                .findByAppName(name)
                .map { appEO -> appEO.toDomain() }
        return ResponseEntity.ok(list)
    }
}

@SpringBootApplication
class SpringKotlinApplication

fun main(args: Array<String>) {
    runApplication<SpringKotlinApplication>(*args)
}
