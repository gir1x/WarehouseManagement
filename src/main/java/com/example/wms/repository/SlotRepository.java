package com.example.wms.repository;

import com.example.wms.domain.Slot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * The full read+write repository. Spring Data generates one proxy bean that
 * implements BOTH JpaRepository and SlotReader — so classes that only ask for
 * the SlotReader type still get this same bean injected, they just can't see
 * the write methods through that narrower reference.
 */
public interface SlotRepository extends JpaRepository<Slot, UUID>, SlotReader {
}
