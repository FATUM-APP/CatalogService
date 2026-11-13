package fatum.catalog.repository

import fatum.catalog.model.ServiceViewEvent
import org.springframework.data.jpa.repository.JpaRepository

interface ServiceViewEventRepository : JpaRepository<ServiceViewEvent, String>
