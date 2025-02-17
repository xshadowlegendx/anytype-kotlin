package com.anytypeio.anytype.ui.multiplayer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.anytypeio.anytype.R
import com.anytypeio.anytype.core_models.primitives.SpaceId
import com.anytypeio.anytype.core_ui.features.multiplayer.ShareSpaceScreen
import com.anytypeio.anytype.core_utils.ext.arg
import com.anytypeio.anytype.core_utils.ext.toast
import com.anytypeio.anytype.core_utils.ui.BaseBottomSheetComposeFragment
import com.anytypeio.anytype.di.common.componentManager
import com.anytypeio.anytype.presentation.multiplayer.ShareSpaceViewModel
import com.anytypeio.anytype.ui.settings.typography
import javax.inject.Inject

class ShareSpaceFragment : BaseBottomSheetComposeFragment() {

    private val space get() = arg<String>(SPACE_ID_KEY)

    @Inject
    lateinit var factory: ShareSpaceViewModel.Factory

    private val vm by viewModels<ShareSpaceViewModel> { factory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme(
                    typography = typography,
                    shapes = MaterialTheme.shapes.copy(medium = RoundedCornerShape(16.dp)),
                ) {
                    ShareSpaceScreen(
                        shareLinkViewState = vm.shareLinkViewState.collectAsStateWithLifecycle().value,
                        isCurrentUserOwner = vm.isCurrentUserOwner.collectAsStateWithLifecycle().value,
                        onRegenerateInviteLinkClicked = vm::onRegenerateInviteLinkClicked,
                        onShareInviteLinkClicked = vm::onShareInviteLinkClicked,
                        members = vm.members.collectAsStateWithLifecycle().value,
                        onViewRequestClicked = vm::onViewRequestClicked,
                        onApproveUnjoinRequestClicked = vm::onApproveUnjoinRequestClicked,
                        onCanEditClicked = vm::onCanEditClicked,
                        onCanViewClicked = vm::onCanViewClicked,
                        onRemoveMemberClicked = vm::onRemoveMemberClicked,
                        onStopSharingClicked = vm::onStopSharingSpaceClicked
                    )
                }
                LaunchedEffect(Unit) {
                    vm.commands.collect { command ->
                        proceedWithCommand(command)
                    }
                }
                LaunchedEffect(Unit) {
                    vm.toasts.collect { toast(it) }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        skipCollapsed()
        expand()
    }

    private fun proceedWithCommand(command: ShareSpaceViewModel.Command) {
        when (command) {
            is ShareSpaceViewModel.Command.ShareInviteLink -> {
                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, command.link)
                    type = "text/plain"
                }
                startActivity(Intent.createChooser(intent, null))
            }
            is ShareSpaceViewModel.Command.ViewJoinRequest -> {
                findNavController().navigate(
                    resId = R.id.spaceJoinRequestScreen,
                    args = SpaceJoinRequestFragment.args(
                        space = command.space,
                        member = command.member
                    )
                )
            }
            is ShareSpaceViewModel.Command.Dismiss -> {
                dismiss()
            }
        }
    }

    override fun injectDependencies() {
        componentManager().shareSpaceComponent.get(SpaceId(space)).inject(this)
    }

    override fun releaseDependencies() {
        componentManager().shareSpaceComponent.release()
    }

    companion object {
        const val SPACE_ID_KEY = "arg.share-space.space-id-key"
        fun args(space: SpaceId) : Bundle = bundleOf(
            SPACE_ID_KEY to space.id
        )
    }
}