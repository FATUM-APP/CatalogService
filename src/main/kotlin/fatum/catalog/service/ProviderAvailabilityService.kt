package fatum.catalog.service

import fatum.catalog.client.UserServiceClient
import fatum.catalog.exception.CatalogErrorCode
import fatum.catalog.exception.FatumServiceException
import fatum.catalog.model.ProviderAvailability
import fatum.catalog.model.constant.Day
import fatum.catalog.repository.ProviderAvailabilityRepository
import org.springframework.transaction.annotation.Transactional
import org.springframework.stereotype.Service
import java.time.LocalTime

@Service
class ProviderAvailabilityService(
    private val providerAvailabilityRepository: ProviderAvailabilityRepository,
    private val userServiceClient: UserServiceClient
) {
    @Transactional
    fun initialize(providerId: String){
        validateProfessional(providerId)
        val days = listOf(
            Day.MONDAY,
            Day.TUESDAY,
            Day.WEDNESDAY,
            Day.THURSDAY,
            Day.FRIDAY,
            Day.SATURDAY,
            Day.SUNDAY
        )
        days.forEach {
            providerAvailabilityRepository.save(ProviderAvailability(providerId, it))
        }
    }

    @Transactional
    fun changeSchedule(
        providerId: String,
        day: Day,
        newInitialTime: LocalTime,
        newFinalTime: LocalTime
    ) {
        validateProfessional(providerId)

        val availability = getAvailability(providerId, day)?.apply {
            changeSchedule(newInitialTime, newFinalTime)
        } ?: ProviderAvailability(
            providerId = providerId,
            day = day,
            initialTime = newInitialTime,
            finalTime = newFinalTime
        )

        providerAvailabilityRepository.save(availability)
    }

    @Transactional
    fun fullDayAvailability(providerId: String, day: Day){
        validateProfessional(providerId)
        val availability = getAvailability(providerId, day)?.apply {
            changeAvailabilityToAllDay()
        } ?: ProviderAvailability(
            providerId = providerId,
            day = day
        )
        providerAvailabilityRepository.save(availability)
    }

    @Transactional(readOnly = true)
    fun findAvailability(providerId: String, day: Day): ProviderAvailability =
        getAvailability(providerId, day)
            ?: throw FatumServiceException(CatalogErrorCode.AVAILABILITY_NOT_FOUND)


    @Transactional(readOnly = true)
    fun findAvailabilityByProvider(providerId: String): List<ProviderAvailability> =
        providerAvailabilityRepository.findByIdProviderId(providerId)

    @Transactional
    fun deleteAvailability(providerId: String, day: Day) {
        providerAvailabilityRepository.delete(
            getAvailability(providerId, day)
                ?: throw FatumServiceException(CatalogErrorCode.AVAILABILITY_NOT_FOUND)
        )
    }

    //==========================================HELPERS==============================================

    private fun validateProfessional(providerId: String){
        val provider = userServiceClient.getUserById(providerId)
        if (!provider.isValid) throw FatumServiceException(CatalogErrorCode.INVALID_USER)
        if (!provider.isProfessional) throw FatumServiceException(CatalogErrorCode.NOT_A_PROFESSIONAL)
    }

    private fun getAvailability(providerId: String, day: Day): ProviderAvailability? =
        providerAvailabilityRepository.findByIdProviderIdAndIdDay(providerId, day)


}