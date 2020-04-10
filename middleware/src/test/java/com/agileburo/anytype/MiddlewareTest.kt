package com.agileburo.anytype

import anytype.Commands.Rpc.Account
import anytype.Commands.Rpc.Block
import anytype.model.Models
import com.agileburo.anytype.common.MockDataFactory
import com.agileburo.anytype.data.auth.model.BlockEntity
import com.agileburo.anytype.data.auth.model.CommandEntity
import com.agileburo.anytype.data.auth.model.PositionEntity
import com.agileburo.anytype.middleware.interactor.Middleware
import com.agileburo.anytype.middleware.interactor.MiddlewareFactory
import com.agileburo.anytype.middleware.interactor.MiddlewareMapper
import com.agileburo.anytype.middleware.service.MiddlewareService
import com.google.protobuf.Value
import com.nhaarman.mockitokotlin2.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import kotlin.test.assertEquals

class MiddlewareTest {

    @Mock
    lateinit var service: MiddlewareService

    private lateinit var middleware: Middleware

    private val mapper = MiddlewareMapper()
    private val factory = MiddlewareFactory()

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        middleware = Middleware(service, factory, mapper)
    }

    @Test
    fun `should call account-stop method when logging out`() {
        middleware.logout()

        val request = Account.Stop.Request.newBuilder().build()

        verify(service, times(1)).accountStop(request)
        verifyNoMoreInteractions(service)
    }

    @Test
    fun `should create request to create new document and return pair of ids`() {

        val command = CommandEntity.CreateDocument(
            context = MockDataFactory.randomUuid(),
            target = MockDataFactory.randomUuid(),
            prototype = BlockEntity.Prototype.Page(
                style = BlockEntity.Content.Page.Style.EMPTY
            ),
            position = PositionEntity.INNER
        )

        val response = Block.CreatePage.Response
            .newBuilder()
            .setBlockId(MockDataFactory.randomUuid())
            .setTargetId(MockDataFactory.randomUuid())
            .build()

        val request = Block.CreatePage.Request
            .newBuilder()
            .setContextId(command.context)
            .setTargetId(command.target)
            .setPosition(Models.Block.Position.Inner)
            .build()

        service.stub {
            on { blockCreatePage(any()) } doReturn response
        }

        val (block, target) = middleware.createDocument(command)

        verify(service, times(1)).blockCreatePage(request)

        assertEquals(
            expected = response.blockId,
            actual = block
        )

        assertEquals(
            expected = response.targetId,
            actual = target
        )
    }

    @Test
    fun `should create a request for replacing target block and return id of the new block`() {

        val command = CommandEntity.Replace(
            context = MockDataFactory.randomUuid(),
            target = MockDataFactory.randomUuid(),
            prototype = BlockEntity.Prototype.Text(
                style = BlockEntity.Content.Text.Style.NUMBERED
            )
        )

        val response = Block.Create.Response
            .newBuilder()
            .setBlockId(MockDataFactory.randomUuid())
            .build()

        val model = Models.Block
            .newBuilder()
            .setText(
                Models.Block.Content.Text
                    .newBuilder()
                    .setStyle(Models.Block.Content.Text.Style.Numbered)
            )

        val request = Block.Create.Request
            .newBuilder()
            .setContextId(command.context)
            .setTargetId(command.target)
            .setPosition(Models.Block.Position.Replace)
            .setBlock(model)
            .build()

        service.stub {
            on { blockCreate(any()) } doReturn response
        }

        val result = middleware.replace(command)

        verify(service, times(1)).blockCreate(request)

        assertEquals(
            expected = response.blockId,
            actual = result
        )
    }

    @Test
    fun `should set emoji icon by updating document details`() {

        val command = CommandEntity.SetIconName(
            context = MockDataFactory.randomUuid(),
            target = MockDataFactory.randomUuid(),
            name = MockDataFactory.randomString()
        )

        val response = Block.Set.Details.Response.getDefaultInstance()

        val key = "icon"

        val value = Value.newBuilder().setStringValue(command.name)

        val details = Block.Set.Details.Detail.newBuilder()
            .setKey(key)
            .setValue(value)

        val request = Block.Set.Details.Request.newBuilder()
            .setContextId(command.context)
            .addDetails(details)
            .build()

        service.stub {
            on { blockSetDetails(any()) } doReturn response
        }

        middleware.setIconName(command)

        verify(service, times(1)).blockSetDetails(request)
        verifyNoMoreInteractions(service)
    }

    @Test
    fun `should set document title by updating document details`() {

        val command = CommandEntity.UpdateTitle(
            context = MockDataFactory.randomUuid(),
            title = MockDataFactory.randomString()
        )

        val response = Block.Set.Details.Response.getDefaultInstance()

        val key = "name"

        val value = Value.newBuilder().setStringValue(command.title)

        val details = Block.Set.Details.Detail.newBuilder()
            .setKey(key)
            .setValue(value)

        val request = Block.Set.Details.Request.newBuilder()
            .setContextId(command.context)
            .addDetails(details)
            .build()

        service.stub {
            on { blockSetDetails(any()) } doReturn response
        }

        middleware.updateDocumentTitle(command)

        verify(service, times(1)).blockSetDetails(request)
        verifyNoMoreInteractions(service)
    }
}