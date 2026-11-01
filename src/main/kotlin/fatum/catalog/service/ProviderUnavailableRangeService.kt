package fatum.catalog.service

import fatum.catalog.client.UserServiceClient
import fatum.catalog.dto.ChangeUnavailabilitySchedule
import fatum.catalog.dto.CreateFullUnavailability
import fatum.catalog.dto.CreateUnavailability
import fatum.catalog.dto.mapper.toEntity
import fatum.catalog.exception.CatalogErrorCode
import fatum.catalog.exception.FatumServiceException
import fatum.catalog.model.ProviderUnavailableRange
import fatum.catalog.repository.UnavailableRangeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth
import java.time.LocalDateTime

@Service
class ProviderUnavailableRangeService(
    private val userServiceClient: UserServiceClient,
    private val unavailableRangeRepository: UnavailableRangeRepository
) {
    @Transactional
    fun establishUnavailability(request : CreateUnavailability){
        val unavailability = request.toEntity()
        validateProfessional(unavailability.providerId)
        validateDates(unavailability.providerId, unavailability.startDate, unavailability.finalDate)
        unavailableRangeRepository.save(unavailability)
    }

    @Transactional
    fun establishFullUnavailability(request: CreateFullUnavailability){
        val unavailability = request.toEntity()
        validateProfessional(unavailability.providerId)
        validateDates(unavailability.providerId, unavailability.startDate, unavailability.finalDate)
        unavailableRangeRepository.save(unavailability)
    }

    @Transactional
    fun changeUnavailabilitySchedule(update : ChangeUnavailabilitySchedule){
        val unavailability = getUnavailability(update.id)
        val start = update.start ?: unavailability.startDate
        val end = update.end ?: unavailability.finalDate
        validateDates(unavailability.providerId, start, end,unavailability.id)
        unavailability.changeDates(start,end)
        unavailableRangeRepository.save(unavailability)
    }

    @Transactional(readOnly = true)
    fun findProviderUnavailableRanges(providerId: String) =
        getProviderUnavailableRanges(providerId)

    @Transactional(readOnly = true)
    fun findProviderUnavailableRangesByMonth(providerId: String, yearMonth: YearMonth)
    : List<ProviderUnavailableRange> {
        val from = yearMonth.atDay(1).atStartOfDay()
        val to = yearMonth.atEndOfMonth().atTime(23, 59, 59, 999_999_999)
        return unavailableRangeRepository.findByProviderIdAndStartDateBetween(providerId, from, to)
    }

    @Transactional
    fun deleteUnavailability(id: String){
        unavailableRangeRepository.delete(getUnavailability(id))
    }


    //==========================================HELPERS==============================================

    private fun validateProfessional(providerId: String){
        val provider = userServiceClient.getUserById(providerId)
        if (!provider.isValid) throw FatumServiceException(CatalogErrorCode.INVALID_USER)
        if (!provider.isProfessional) throw FatumServiceException(CatalogErrorCode.NOT_A_PROFESSIONAL)
    }

    // validateDates — agregar parámetro excludeId
    private fun validateDates(
        providerId: String,
        start: LocalDateTime,
        end: LocalDateTime,
        excludeId: String? = null
    ) {
        val unavailability = getProviderUnavailableRanges(providerId)
        val isInRange = unavailability.any { existing ->
            existing.id != excludeId &&
                    !start.isAfter(existing.finalDate) &&
                    !end.isBefore(existing.startDate)
        }

        if (isInRange) throw FatumServiceException(CatalogErrorCode.OVERLAPPING_RANGE)
    }

    private fun getUnavailability(id : String): ProviderUnavailableRange{
        return unavailableRangeRepository.findById(id).orElse(null)
            ?: throw FatumServiceException(CatalogErrorCode.UNAVAILABILITY_NOT_FOUND)
    }



    private fun getProviderUnavailableRanges(providerId: String): List<ProviderUnavailableRange> =
        unavailableRangeRepository.findByProviderId(providerId)

}