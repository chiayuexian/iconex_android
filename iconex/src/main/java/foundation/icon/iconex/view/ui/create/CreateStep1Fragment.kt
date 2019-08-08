package foundation.icon.iconex.view.ui.create

import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import foundation.icon.iconex.R

class CreateStep1Fragment : Fragment() {

    companion object {
        fun newInstance() = CreateStep1Fragment()
    }

    private lateinit var viewModel: CreateViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.create_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(CreateViewModel::class.java)
        // TODO: Use the ViewModel
    }

}