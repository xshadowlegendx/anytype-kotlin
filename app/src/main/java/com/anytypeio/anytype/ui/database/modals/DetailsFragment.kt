package com.anytypeio.anytype.ui.database.modals

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.anytypeio.anytype.R
import com.anytypeio.anytype.core_ui.layout.ListDividerItemDecoration
import com.anytypeio.anytype.core_utils.ui.BaseBottomSheetFragment
import com.anytypeio.anytype.di.common.componentManager
import com.anytypeio.anytype.di.feature.DetailsModule
import com.anytypeio.anytype.presentation.databaseview.modals.DetailsViewModel
import com.anytypeio.anytype.presentation.databaseview.modals.DetailsViewModelFactory
import com.anytypeio.anytype.presentation.databaseview.modals.DetailsViewState
import com.anytypeio.anytype.ui.database.modals.ModalsNavFragment.Companion.ARGS_DB_ID
import com.anytypeio.anytype.ui.database.modals.adapter.DetailsAdapter
import kotlinx.android.synthetic.main.modal_properties.*
import javax.inject.Inject

@Deprecated("legacy?")
class DetailsFragment : BaseBottomSheetFragment() {

    companion object {
        fun newInstance(id: String): DetailsFragment =
            DetailsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARGS_DB_ID, id)
                }
            }
    }

    @Inject
    lateinit var factory: DetailsViewModelFactory

    private val vm : DetailsViewModel by viewModels { factory }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.modal_properties, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vm.state.observe(viewLifecycleOwner, Observer { render(it) })
        vm.onViewCreated()
    }

    private fun render(state: DetailsViewState) {
        when (state) {
            is DetailsViewState.Init -> {
                iconBack.setOnClickListener { vm.onBackClick() }
                reorder.setOnClickListener { vm.onReorderClick() }
                with(recyclerProperties) {
                    layoutManager = LinearLayoutManager(requireContext())
                    addItemDecoration(ListDividerItemDecoration(requireContext()))
                    adapter =
                        DetailsAdapter(
                            {},
                            state.details.toMutableList(),
                            vm::onDetailClick
                        )
                }
            }
            is DetailsViewState.NavigateToCustomize -> {
                (parentFragment as ModalNavigation).showCustomizeScreen()
            }
            is DetailsViewState.NavigateToEditDetail -> {
                (parentFragment as ModalNavigation).showDetailEditScreen(
                    detailId = state.detailId
                )
            }
            is DetailsViewState.NavigateToReorder -> {
                (parentFragment as ModalNavigation).showReorderDetails()
            }
        }
    }

    override fun injectDependencies() {
        componentManager()
            .mainComponent
            .detailsBuilder()
            .propertiesModule(DetailsModule(id = arguments?.getString(ARGS_DB_ID) as String))
            .build()
            .inject(this)
    }

    override fun releaseDependencies() {}
}