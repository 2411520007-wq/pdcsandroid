package com.a2411520007.mahasiswa

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.a2411520007.mahasiswa.adapter.MahasiswaAdapter
import com.a2411520007.mahasiswa.api.ApiConfig
import com.a2411520007.mahasiswa.databinding.ActivityMainBinding
import com.a2411520007.mahasiswa.databinding.DialogDetailMahasiswaBinding
import com.a2411520007.mahasiswa.databinding.DialogKonfirmasiHapusBinding
import com.a2411520007.mahasiswa.model.Mahasiswa
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: MahasiswaAdapter

    private val tambahLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadData()
        }
    }

    private val editLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            loadData()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupActions()
        loadData()
    }

    private fun setupRecyclerView() {
        adapter = MahasiswaAdapter(
            onItemClick = { mahasiswa -> showDetail(mahasiswa.id) },
            onEdit = { mahasiswa -> bukaEdit(mahasiswa) },
            onHapus = { mahasiswa -> konfirmasiHapus(mahasiswa) }
        )
        binding.rvMahasiswa.layoutManager = LinearLayoutManager(this)
        binding.rvMahasiswa.adapter = adapter
    }

    private fun setupActions() {
        binding.btnTambah.setOnClickListener {
            tambahLauncher.launch(Intent(this, TambahMahasiswaActivity::class.java))
        }

        binding.btnCari.setOnClickListener {
            val keyword = binding.etSearch.text.toString().trim()
            loadData(keyword.ifEmpty { null })
        }

        binding.btnReset.setOnClickListener {
            binding.etSearch.text?.clear()
            loadData()
        }
    }

    private fun bukaEdit(m: Mahasiswa) {
        val intent = Intent(this, EditMahasiswaActivity::class.java).apply {
            putExtra(EditMahasiswaActivity.EXTRA_ID, m.id)
            putExtra(EditMahasiswaActivity.EXTRA_NAMA, m.nama)
            putExtra(EditMahasiswaActivity.EXTRA_NIM, m.nim)
            putExtra(EditMahasiswaActivity.EXTRA_JURUSAN, m.jurusan)
        }
        editLauncher.launch(intent)
    }

    private fun konfirmasiHapus(m: Mahasiswa) {
        val dialogBinding = DialogKonfirmasiHapusBinding.inflate(layoutInflater)
        dialogBinding.tvHapusJudul.text = "Hapus ${m.nama}?"
        dialogBinding.tvHapusPesan.text =
            "Yakin hapus data dengan NIM ${m.nim}? Tindakan ini tidak bisa dibatalkan."

        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogBinding.btnBatalHapus.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnKonfirmasiHapus.setOnClickListener {
            dialog.dismiss()
            hapus(m.id)
        }
        dialog.show()
    }

    private fun hapus(id: String) {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiConfig.getApiService().deleteMahasiswa(id)
                }

                if (response.isSuccessful && response.body()?.status == true) {
                    UiPopup.show(this@MainActivity, "Dihapus", isError = false)
                    loadData()
                } else {
                    val code = response.code()
                    val msg = if (response.body() != null) {
                        response.body()?.message ?: response.message()
                    } else {
                        parseErrorMessage(response)
                    }
                    UiPopup.show(
                        this@MainActivity,
                        "Gagal hapus ($code): $msg",
                        isError = true,
                        durationMs = 3200L
                    )
                }
            } catch (e: Exception) {
                UiPopup.show(
                    this@MainActivity,
                    "Gagal hapus (${e.localizedMessage})",
                    isError = true,
                    durationMs = 3200L
                )
            }
        }
    }

    private fun parseErrorMessage(response: Response<*>): String {
        return try {
            val raw = response.errorBody()?.string()
            if (raw.isNullOrEmpty()) return response.message()
            JSONObject(raw).optString("message", response.message())
        } catch (_: Exception) {
            response.message()
        }
    }

    private fun loadData(keyword: String? = null) {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    if (keyword.isNullOrEmpty()) {
                        ApiConfig.getApiService().getDaftarMahasiswa()
                    } else {
                        ApiConfig.getApiService().searchMahasiswa(keyword)
                    }
                }

                if (response.isSuccessful) {
                    val list = response.body()?.data.orEmpty()
                    adapter.updateData(list)
                    binding.tvTitle.text = "Data Mahasiswa (${list.size})"

                    if (list.isEmpty()) {
                        binding.tvEmpty.visibility = View.VISIBLE
                        binding.tvEmpty.text = if (keyword.isNullOrEmpty()) {
                            "Belum ada data"
                        } else {
                            "Tidak ditemukan untuk \"$keyword\""
                        }
                    } else {
                        binding.tvEmpty.visibility = View.GONE
                    }
                } else {
                    UiPopup.show(
                        this@MainActivity,
                        "Gagal memuat (HTTP ${response.code()})",
                        isError = true
                    )
                    binding.tvEmpty.visibility = View.VISIBLE
                    binding.tvEmpty.text = "Gagal memuat (HTTP ${response.code()})"
                }
            } catch (e: SocketTimeoutException) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.tvEmpty.text = "Timeout: periksa IP & port server."
            } catch (e: IOException) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.tvEmpty.text = "Gagal terhubung (${e.localizedMessage})"
            } catch (e: HttpException) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.tvEmpty.text = "Kesalahan HTTP (${e.code()})"
            } catch (e: Exception) {
                binding.tvEmpty.visibility = View.VISIBLE
                binding.tvEmpty.text = "Terjadi kesalahan (${e.localizedMessage})"
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun showDetail(id: String) {
        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    ApiConfig.getApiService().getDetailMahasiswa(id)
                }

                if (response.isSuccessful) {
                    val m = response.body()?.data
                    if (m != null) {
                        val dialogBinding = DialogDetailMahasiswaBinding.inflate(layoutInflater)
                        dialogBinding.tvDetailNama.text = m.nama
                        dialogBinding.tvDetailId.text = m.id
                        dialogBinding.tvDetailNim.text = m.nim
                        dialogBinding.tvDetailJurusan.text = m.jurusan

                        val dialog = AlertDialog.Builder(this@MainActivity)
                            .setView(dialogBinding.root)
                            .create()

                        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                        dialogBinding.btnTutup.setOnClickListener { dialog.dismiss() }
                        dialog.show()
                    } else {
                        UiPopup.show(this@MainActivity, "Data tidak ditemukan", isError = true)
                    }
                } else {
                    UiPopup.show(
                        this@MainActivity,
                        "Detail gagal (HTTP ${response.code()})",
                        isError = true
                    )
                }
            } catch (e: Exception) {
                UiPopup.show(
                    this@MainActivity,
                    "Gagal memuat detail (${e.localizedMessage})",
                    isError = true
                )
            }
        }
    }
}