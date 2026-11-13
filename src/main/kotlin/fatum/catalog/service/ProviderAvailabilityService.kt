package fatum.catalog.service

import fatum.catalog.client.UserServiceClient
import fatum.catalog.dto.mapper.toEntity
import fatum.catalog.dto.request.schedule.ChangeAvailability
import fatum.catalog.dto.request.schedule.FullAvailability
import fatum.catalog.exception.CatalogErrorCode
import fatum.catalog.exception.FatumServiceException
import fatum.catalog.model.ProviderAvailability
import fatum.catalog.realtime.CalendarChange
import fatum.catalog.realtime.CalendarRealtimePublisher
import fatum.catalog.repository.ProviderAvailabilityRepository
import org.springframework.transaction.annotation.Transactional
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalTime

@Service
class ProviderAvailabilityService(
    private val providerAvailabilityRepository: ProviderAvailabilityRepository,
    private val userServiceClient: UserServiceClient,
    private val calendar: CalendarRealtimePublisher
) {
    @Transactional
    fun initialize(providerId: String){
        validateProfessional(providerId)
        val days = listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY
        )
        days.forEach {
            providerAvailabilityRepository.save(ProviderAvailability(providerId, it))
        }
        calendar.changed(providerId, CalendarChange.WEEKLY_SCHEDULE_CHANGED)
    }

    @Transactional
    fun changeSchedule(
        request : ChangeAvailability
    ) {
        validateProfessional(request.providerId)

        val availability = getAvailability(request.providerId, request.day)?.apply {
            changeSchedule(request.start, request.end)
        } ?: request.toEntity()

        providerAvailabilityRepository.save(availability)
        calendar.changed(request.providerId, CalendarChange.WEEKLY_SCHEDULE_CHANGED)
    }

    @Transactional
    fun fullDayAvailability(request : FullAvailability){
        validateProfessional(request.providerId)
        val availability = getAvailability(request.providerId, request.day)?.apply {
            changeAvailabilityToAllDay()
        } ?: request.toEntity()
        providerAvailabilityRepository.save(availability)
        calendar.changed(request.providerId, CalendarChange.WEEKLY_SCHEDULE_CHANGED)
    }

    @Transactional(readOnly = true)
    fun findAvailability(providerId: String, day: DayOfWeek): ProviderAvailability =
        getAvailability(providerId, day)
            ?: throw FatumServiceException(CatalogErrorCode.AVAILABILITY_NOT_FOUND)

    @Transactional(readOnly = true)
    fun providerIsAvailable(providerId: String, day: DayOfWeek, time: LocalTime): Boolean =
        getAvailability(providerId, day)?.let {
            !time.isBefore(it.initialTime) && !time.isAfter(it.finalTime)
        } ?: false


    @Transactional(readOnly = true)
    fun findAvailabilityByProvider(providerId: String): List<ProviderAvailability> =
        providerAvailabilityRepository.findByIdProviderId(providerId)

    @Transactional
    fun deleteAvailability(providerId: String, day: DayOfWeek) {
        providerAvailabilityRepository.delete(
            getAvailability(providerId, day)
                ?: throw FatumServiceException(CatalogErrorCode.AVAILABILITY_NOT_FOUND)
        )
        calendar.changed(providerId, CalendarChange.WEEKLY_SCHEDULE_CHANGED)
    }

    //==========================================HELPERS==============================================

    private fun validateProfessional(providerId: String){
        val provider = userServiceClient.getUserById(providerId)
        if (!provider.isValid) throw FatumServiceException(CatalogErrorCode.INVALID_USER)
        if (!provider.isProfessional) throw FatumServiceException(CatalogErrorCode.NOT_A_PROFESSIONAL)
    }

    private fun getAvailability(providerId: String, day: DayOfWeek): ProviderAvailability? =
        providerAvailabilityRepository.findByIdProviderIdAndIdDay(providerId, day)


}