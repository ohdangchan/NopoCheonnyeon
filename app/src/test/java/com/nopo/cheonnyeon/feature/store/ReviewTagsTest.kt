package com.nopo.cheonnyeon.feature.store

import org.junit.Assert.assertEquals
import org.junit.Test

class ReviewTagsTest {
    @Test fun `칩이 없으면 글만`() = assertEquals("맛있어요", withTags(" 맛있어요 ", emptyList()))

    @Test fun `글 끝에 한 줄로 붙는다`() = assertEquals("맛있어요\n#분위기 #재방문의사", withTags("맛있어요", listOf("분위기", "재방문 의사")))

    @Test fun `글이 비어도 칩만 남는다`() = assertEquals("#맛", withTags("", listOf("맛")))
}
