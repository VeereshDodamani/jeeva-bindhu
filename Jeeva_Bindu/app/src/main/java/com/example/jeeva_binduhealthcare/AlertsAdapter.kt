package com.example.jeeva_binduhealthcare

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.jeeva_binduhealthcare.databinding.ItemEmergencyAlertBinding

class AlertsAdapter(
    private var alerts: List<EmergencyRequest>,
    private val onComingClicked: (EmergencyRequest) -> Unit,
    private val onFindDonorsClicked: (EmergencyRequest) -> Unit
) : RecyclerView.Adapter<AlertsAdapter.AlertViewHolder>() {

    class AlertViewHolder(val binding: ItemEmergencyAlertBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertViewHolder {
        val binding = ItemEmergencyAlertBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AlertViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlertViewHolder, position: Int) {
        val alert = alerts[position]
        holder.binding.tvBloodGroup.text = "${alert.bloodGroupRequired} REQUIRED"
        holder.binding.tvHospitalName.text = alert.hospitalName
        holder.binding.tvLocation.text = alert.location
        holder.binding.tvResponders.text = "Responders: ${alert.responders.size}"

        holder.binding.btnImComing.setOnClickListener {
            onComingClicked(alert)
        }

        holder.binding.btnFindDonors.setOnClickListener {
            onFindDonorsClicked(alert)
        }
    }

    override fun getItemCount(): Int = alerts.size

    fun updateData(newList: List<EmergencyRequest>) {
        alerts = newList
        notifyDataSetChanged()
    }
}
