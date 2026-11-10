package com.example.backend.title;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppTitleRepository extends JpaRepository<AppTitleEntity, Long> {

}
