package com.cekgigi.app.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cekgigi.app.R
import com.cekgigi.app.data.ScreeningEntity
import com.cekgigi.app.databinding.ItemRiwayatBinding
import java.io.File

class RiwayatAdapter(
    private var data: List<ScreeningEntity>
) : RecyclerView.Adapter<RiwayatAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemRiwayatBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRiwayatBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        val context = holder.itemView.context

        holder.binding.txtTanggal.text = item.tanggal
        holder.binding.txtStatus.text = if (item.statusGigi == "Sehat") {
            "Gigi Bersih & Sehat"
        } else {
            item.kelasDeteksi
        }
        holder.binding.txtAkurasi.text = "Akurasi: ${String.format("%.1f", item.akurasi)}%"

        // Badge
        if (item.statusGigi == "Sehat") {
            holder.binding.txtBadge.text = "SEHAT"
            holder.binding.txtBadge.setBackgroundResource(R.drawable.bg_badge_sehat)
            holder.binding.txtBadge.setTextColor(context.getColor(R.color.success_green))
        } else {
            holder.binding.txtBadge.text = "BERMASALAH"
            holder.binding.txtBadge.setBackgroundResource(R.drawable.bg_badge_tidak_sehat)
            holder.binding.txtBadge.setTextColor(context.getColor(R.color.warning_amber))
        }

        // Thumbnail dengan Glide
        Glide.with(context)
            .load(File(item.pathFoto))
            .placeholder(R.drawable.ic_tooth)
            .error(R.drawable.ic_tooth)
            .centerCrop()
            .into(holder.binding.imgThumbnail)
    }

    override fun getItemCount(): Int = data.size

    fun updateData(newData: List<ScreeningEntity>) {
        data = newData
        notifyDataSetChanged()
    }
}
