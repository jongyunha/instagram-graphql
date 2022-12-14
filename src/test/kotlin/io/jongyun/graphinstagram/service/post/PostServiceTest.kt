package io.jongyun.graphinstagram.service.post

import com.github.javafaker.Faker
import io.jongyun.graphinstagram.entity.hashtag.HashtagRepository
import io.jongyun.graphinstagram.entity.member.Member
import io.jongyun.graphinstagram.entity.member.MemberRepository
import io.jongyun.graphinstagram.entity.post.Post
import io.jongyun.graphinstagram.entity.post.PostCustomRepository
import io.jongyun.graphinstagram.entity.post.PostRepository
import io.jongyun.graphinstagram.exception.BusinessException
import io.jongyun.graphinstagram.exception.ErrorCode
import io.jongyun.graphinstagram.types.CreatePostInput
import io.jongyun.graphinstagram.types.UpdatePostInput
import io.jongyun.graphinstagram.util.mapToGraphql
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import java.time.OffsetDateTime
import java.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.jongyun.graphinstagram.types.Post as TypesPost


val faker = Faker()

@ExperimentalKotest
class PostServiceTest : BehaviorSpec({
    val postRepository: PostRepository = mockk()
    val memberRepository: MemberRepository = mockk()
    val postCustomRepository: PostCustomRepository = mockk()
    val hashtagRepository: HashtagRepository = mockk()
    val postService = PostService(postRepository, memberRepository, postCustomRepository, hashtagRepository)
    lateinit var member: Member
    lateinit var post: Post
    lateinit var updatePostInput: UpdatePostInput

    beforeTest {
        member = generateMember()
        post = Post(content = "테스트 입니다.", createdBy = member)
        updatePostInput = UpdatePostInput("1", "업데이트 테스트")
    }

    afterTest {
        clearAllMocks()
    }

    given("post content 가 비어있어서") {
        val createPostInput = CreatePostInput(content = "")
        Then("예외를 반환한다.") {
            shouldThrow<BusinessException> { postService.createPost(1L, createPostInput) }
        }
    }

    given("post content 가 있어서") {
        val hashtagList = listOf("#안녕", "#테스트")
        val createPostInput = CreatePostInput(content = "테스트 컨텐츠", tags = hashtagList)
        Then("성공한다.") {
            every { memberRepository.findById(1L) } returns Optional.of(member)
            every { postRepository.save(any()) } returns post
            every { hashtagRepository.findAllByTagNameIn(hashtagList) } returns emptyList()
            postService.createPost(1L, createPostInput) shouldBe true
        }
    }

    given("post 의 content 업데이트 시 member 의 ID 를 1로 설정한다.") {
        val memberId = 1L
        `when`("member id 1에 대한 member 를 찾을 수 없다.") {
            every { memberRepository.findById(memberId) } returns Optional.empty()
            Then("member 를 찾지 못해 예외가 발생한다.") {
                shouldThrow<BusinessException> { postService.updatePost(memberId, updatePostInput) }
            }
        }
        `when`("post id 에 대한 post 를 찾을 수 없다.") {
            every { memberRepository.findById(memberId) } returns Optional.of(member)
            every { postRepository.findByCreatedByAndId(member, updatePostInput.postId.toLong()) } returns null
            Then("post 를 찾지 못해 예외가 발생한다.") {
                shouldThrow<BusinessException> { postService.updatePost(memberId, updatePostInput) }
            }
        }
        `when`("member 와 post 모두 정상적으로 조회") {
            every { memberRepository.findById(memberId) } returns Optional.of(member)
            every { postRepository.findByCreatedByAndId(member, updatePostInput.postId.toLong()) } returns post
            every { postRepository.save(post) } returns post
            Then("정상적으로 업데이트 된다.") {
                postService.updatePost(memberId, updatePostInput) shouldBe true
            }
        }
    }

    Given("post 삭제시 post ID 를 1로 설정한다.") {
        val postId = 1L
        val memberId = 1L
        When("member id 에 대한 member 를 찾을 수없다.") {
            every { memberRepository.findById(memberId) } returns Optional.empty()
            Then("member 를 찾지못해 예외가 발생한다.") {
                val result = shouldThrow<BusinessException> { postService.deletePost(memberId, postId) }
                result.errorCode shouldBe ErrorCode.MEMBER_DOES_NOT_EXISTS
            }
        }
        When("post id 에 대한 post 찾을 수 없다.") {
            every { memberRepository.findById(memberId) } returns Optional.of(member)
            every { postRepository.findByCreatedByAndId(member, postId) } returns null
            Then("post 를 찾지 못해 예외가 발생한다.") {
                val result = shouldThrow<BusinessException> { postService.deletePost(memberId, postId) }
                result.errorCode shouldBe ErrorCode.POST_DOES_NOT_EXISTS
            }
        }
        When("모든 조건을 통과한다.") {
            every { memberRepository.findById(memberId) } returns Optional.of(member)
            every { postRepository.findByCreatedByAndId(member, postId) } returns post
            every { postRepository.delete(post) } returns Unit
            Then("성공한다.") {
                val result = withContext(Dispatchers.IO) {
                    postService.deletePost(memberId, postId)
                }
                result shouldBe true
            }
        }
    }


})

fun generateTypePost(member: Member): TypesPost {
    return TypesPost(
        id = faker.idNumber().toString(),
        createdBy = mapToGraphql(member),
        content = faker.book().title(),
        createdAt = OffsetDateTime.now(),
        updatedAt = OffsetDateTime.now(),
    )
}

fun generateMember(): Member {
    return Member(
        faker.name().toString(), faker.hashCode().toString()
    )
}