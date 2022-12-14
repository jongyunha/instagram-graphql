package io.jongyun.graphinstagram.entity.member

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MemberRepository : JpaRepository<Member, Long> {

    fun existsByName(name: String): Boolean

    fun findByName(name: String): Member?

    fun findAllByIdIn(ids: List<Long>): List<Member>
}