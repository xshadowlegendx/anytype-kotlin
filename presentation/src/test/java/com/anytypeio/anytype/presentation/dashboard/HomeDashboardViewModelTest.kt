package com.anytypeio.anytype.presentation.dashboard

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.anytypeio.anytype.analytics.base.Analytics
import com.anytypeio.anytype.core_models.Block
import com.anytypeio.anytype.core_models.Config
import com.anytypeio.anytype.core_models.Event
import com.anytypeio.anytype.core_models.Payload
import com.anytypeio.anytype.core_models.Relations
import com.anytypeio.anytype.core_models.SmartBlockType
import com.anytypeio.anytype.core_models.StubConfig
import com.anytypeio.anytype.core_models.ext.getChildrenIdsList
import com.anytypeio.anytype.domain.`object`.ObjectTypesProvider
import com.anytypeio.anytype.domain.auth.interactor.GetProfile
import com.anytypeio.anytype.domain.base.Either
import com.anytypeio.anytype.domain.base.Resultat
import com.anytypeio.anytype.domain.block.interactor.Move
import com.anytypeio.anytype.domain.config.DebugSettings
import com.anytypeio.anytype.domain.config.Gateway
import com.anytypeio.anytype.domain.config.GetConfig
import com.anytypeio.anytype.domain.config.GetDebugSettings
import com.anytypeio.anytype.domain.dashboard.interactor.CloseDashboard
import com.anytypeio.anytype.domain.dashboard.interactor.OpenDashboard
import com.anytypeio.anytype.domain.event.interactor.InterceptEvents
import com.anytypeio.anytype.domain.misc.UrlBuilder
import com.anytypeio.anytype.domain.objects.DefaultStoreOfObjectTypes
import com.anytypeio.anytype.domain.objects.DeleteObjects
import com.anytypeio.anytype.domain.objects.ObjectStore
import com.anytypeio.anytype.domain.objects.SetObjectListIsArchived
import com.anytypeio.anytype.domain.page.CreateObject
import com.anytypeio.anytype.domain.search.CancelSearchSubscription
import com.anytypeio.anytype.domain.search.ObjectSearchSubscriptionContainer
import com.anytypeio.anytype.domain.search.SearchObjects
import com.anytypeio.anytype.presentation.MockBlockContentFactory.StubLinkContent
import com.anytypeio.anytype.presentation.MockBlockFactory.link
import com.anytypeio.anytype.presentation.navigation.AppNavigation
import com.anytypeio.anytype.presentation.util.CoroutinesTestRule
import com.anytypeio.anytype.test_utils.MockDataFactory
import com.jraska.livedata.test
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import kotlin.test.assertContains
import kotlin.test.assertEquals

class HomeDashboardViewModelTest {

    @get:Rule
    val rule = InstantTaskExecutorRule()

    @get:Rule
    val coroutineTestRule = CoroutinesTestRule()

    @Mock
    lateinit var getProfile: GetProfile

    @Mock
    lateinit var openDashboard: OpenDashboard

    @Mock
    lateinit var getConfig: GetConfig

    @Mock
    lateinit var closeDashboard: CloseDashboard

    @Mock
    lateinit var searchObjects: SearchObjects

    @Mock
    lateinit var setObjectListIsArchived: SetObjectListIsArchived

    @Mock
    lateinit var deleteObjects: DeleteObjects

    @Mock
    lateinit var interceptEvents: InterceptEvents

    @Mock
    lateinit var getDebugSettings: GetDebugSettings

    @Mock
    lateinit var move: Move

    @Mock
    lateinit var gateway: Gateway

    @Mock
    lateinit var analytics: Analytics

    @Mock
    lateinit var objectTypesProvider: ObjectTypesProvider

    @Mock
    lateinit var cancelSearchSubscription: CancelSearchSubscription

    @Mock
    lateinit var objectSearchSubscriptionContainer: ObjectSearchSubscriptionContainer

    @Mock
    lateinit var objectStore: ObjectStore

    @Mock
    lateinit var createObject: CreateObject

    private lateinit var vm: HomeDashboardViewModel

    private val config = StubConfig()

    private val builder: UrlBuilder get() = UrlBuilder(gateway)

    private val storeOfObjectTypes = DefaultStoreOfObjectTypes()

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    private fun givenViewModel(): HomeDashboardViewModel {
        return HomeDashboardViewModel(
            getProfile = getProfile,
            openDashboard = openDashboard,
            closeDashboard = closeDashboard,
            getConfig = getConfig,
            move = move,
            interceptEvents = interceptEvents,
            eventConverter = HomeDashboardEventConverter.DefaultConverter(
                builder = builder,
                storeOfObjectTypes = storeOfObjectTypes
            ),
            getDebugSettings = getDebugSettings,
            analytics = analytics,
            searchObjects = searchObjects,
            deleteObjects = deleteObjects,
            setObjectListIsArchived = setObjectListIsArchived,
            urlBuilder = builder,
            cancelSearchSubscription = cancelSearchSubscription,
            objectStore = objectStore,
            objectSearchSubscriptionContainer = objectSearchSubscriptionContainer,
            createObject = createObject,
            featureToggles = mock(),
            storeOfObjectTypes = storeOfObjectTypes
        )
    }

    @Test
    fun `key set for search subscription should contain done relation - when request`() {
        givenViewModel().onStart()

        captureObserveKeys(objectSearchSubscriptionContainer).allValues.forEach { keys ->
            assertContains(keys, Relations.DONE, "Should contain done relation for tasks!")
        }
    }

    private fun captureObserveKeys(spannable: ObjectSearchSubscriptionContainer): KArgumentCaptor<List<String>> {
        val captor = argumentCaptor<List<String>>()
        verify(spannable, atLeast(3)).observe(
            subscription = any(),
            sorts = any(),
            filters = any(),
            source = any(),
            offset = any(),
            limit = any(),
            keys = captor.capture()
        )
        return captor
    }

    @Test
    fun `should only start getting config when view model is initialized`() {

        // SETUP

        val response = Either.Right(config)

        stubGetConfig(response)
        stubObserveEvents(params = InterceptEvents.Params(context = null))
        stubObserveProfile()

        // TESTING

        vm = givenViewModel()

        verify(getConfig, times(1)).invoke(any(), any(), any())
        verifyNoInteractions(openDashboard)
    }

    @Test
    fun `should start observing events after receiving config`() {

        // SETUP

        val response = Either.Right(config)

        val params = InterceptEvents.Params(context = config.home)

        stubGetConfig(response)
        stubObserveEvents(params = params)

        // TESTING

        vm = givenViewModel()

        verify(getConfig, times(1)).invoke(any(), any(), any())
        verify(interceptEvents, times(1)).build(params)
    }

    @Test
    fun `should start observing home dashboard after receiving config`() {

        // SETUP

        val response = Either.Right(config)

        stubGetConfig(response)
        stubObserveEvents(params = InterceptEvents.Params(context = config.home))
        stubObserveProfile()

        // TESTING

        vm = givenViewModel()

        verify(getConfig, times(1)).invoke(any(), any(), any())
        verifyNoInteractions(openDashboard)
    }

    @Test
    fun `should emit loading state when home dashboard loading started`() {

        // SETUP

        stubGetConfig(Either.Right(config))
        stubObserveEvents(params = InterceptEvents.Params(context = config.home))
        stubOpenDashboard()

        // TESTING

        vm = givenViewModel()

        vm.onViewCreated()

        val expected = HomeDashboardStateMachine.State(
            isLoading = true,
            isInitialzed = true,
            blocks = emptyList(),
            childrenIdsList = emptyList(),
            error = null
        )

        vm.state.test().assertValue(expected)
    }

    @Test
    fun `should emit view state with dashboard when home dashboard loading started`() {

        // SETUP

        val targetId = MockDataFactory.randomUuid()

        val page = link(
            content = StubLinkContent(target = targetId)
        )

        val dashboard = Block(
            id = config.home,
            content = Block.Content.Smart(SmartBlockType.HOME),
            children = listOf(page.id),
            fields = Block.Fields.empty()
        )

        stubGetConfig(Either.Right(config))

        stubObserveEvents(params = InterceptEvents.Params(context = config.home))

        stubOpenDashboard(
            payload = Payload(
                context = config.home,
                events = listOf(
                    Event.Command.ShowObject(
                        root = config.home,
                        context = config.home,
                        blocks = listOf(dashboard, page),
                        type = SmartBlockType.HOME
                    )
                )
            )
        )

        // TESTING

        vm = givenViewModel()

        vm.onViewCreated()

        val views = listOf<DashboardView>(
            DashboardView.Document(
                id = page.id,
                target = targetId,
                isArchived = false,
                isLoading = true
            )
        )

        val expected =  HomeDashboardStateMachine.State(
            isLoading = false,
            isInitialzed = true,
            blocks = views,
            childrenIdsList = listOf(dashboard).getChildrenIdsList(dashboard.id),
            error = null
        )
        val actual = vm.state.test().value()
        assertEquals(expected = expected, actual = actual)

        coroutineTestRule.advanceTime(200L)
    }

    @Test
    fun `should proceed opening dashboard when view is created`() {

        // SETUP

        stubGetConfig(Either.Right(config))
        stubObserveEvents(params = InterceptEvents.Params(context = config.home))
        stubOpenDashboard()
        stubObserveProfile()

        // TESTING

        vm = givenViewModel()
        vm.onViewCreated()

        runBlockingTest {
            verify(openDashboard, times(1)).execute(eq(Unit))
        }
    }

    @Test
    fun `should close dashboard and navigate to page screen when page is created`() {

        val id = MockDataFactory.randomUuid()

        stubObserveEvents()
        stubGetEditorSettings()
        stubCloseDashboard()

        vm = givenViewModel()

        givenDelegateId(id)
        vm.onAddNewDocumentClicked()

        vm.navigation
            .test()
            .assertHasValue()
            .assertValue { value ->
                (value.peekContent() as AppNavigation.Command.OpenObject).id == id
            }
    }

    private fun givenDelegateId(id: String) {
        createObject.stub {
            onBlocking { execute(CreateObject.Param(null)) } doReturn Resultat.success(
                CreateObject.Result(
                    objectId = id,
                    event = Payload(
                        context = id,
                        events = listOf()
                    )
                )
            )
        }
    }

    private fun stubGetConfig(response: Either.Right<Config>) {
        getConfig.stub {
            onBlocking { invoke(any(), any(), any()) } doAnswer { answer ->
                answer.getArgument<(Either<Throwable, Config>) -> Unit>(2)(response)
            }
        }
    }

    private fun stubObserveEvents(
        flow: Flow<List<Event>> = flowOf(),
        params: InterceptEvents.Params? = null
    ) {
        interceptEvents.stub {
            onBlocking { build(params) } doReturn flow
        }
    }

    private fun stubOpenDashboard(
        payload: Payload = Payload(
            context = MockDataFactory.randomString(),
            events = emptyList()
        )
    ) {
        openDashboard.stub {
            onBlocking { execute(params = Unit) } doReturn Resultat.success(payload)
        }
    }

    private fun stubCloseDashboard() {
        closeDashboard.stub {
            onBlocking { invoke(any(), any(), any()) } doAnswer { answer ->
                answer.getArgument<(Either<Throwable, Unit>) -> Unit>(2)(Either.Right(Unit))
            }
        }
    }

    private fun stubGetEditorSettings() {
        getDebugSettings.stub {
            onBlocking { invoke(any()) } doReturn Either.Right(DebugSettings(true))
        }
    }

    private fun stubObserveProfile() {
        getProfile.stub {
            on {
                observe(
                    subscription = any(),
                    keys = any(),
                    dispatcher = any()
                )
            } doReturn emptyFlow()
        }
    }
}