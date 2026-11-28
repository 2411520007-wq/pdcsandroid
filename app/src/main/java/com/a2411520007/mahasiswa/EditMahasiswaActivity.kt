package com.a2411520007.mahasiswa

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.a2411520007.mahasiswa.api.ApiConfig
import com.a2411520007.mahasiswa.databinding.ActivityEditMahasiswaBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

class EditMahasiswaActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "EXTRA_ID"
        const val EXTRA_NAMA = "EXTRA_NAMA"
        const val EXTRA_NIM = "EXTRA_NIM"
        const val EXTRA_JURUSAN = "EXTRA_JURUSAN"
    }

    private lateinit var binding: ActivityEditMahasiswaBinding
    private var mahasiswaId: String = ""

    private fun parseErrorMessage(response: Response<*>): String {
        return try {
            val raw = response.errorBody()?.string()
            if (raw.isNullOrEmpty()) return response.message()
            JSONObject(raw).optString("message", response.message())
        } catch (_: Exception) {
            response.message()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditMahasiswaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Edit Mahasiswa"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mahasiswaId = intent.getStringExtra(EXTRA_ID).orEmpty()
        if (mahasiswaId.isEmpty()) {
            UiPopup.show(this, "ID tidak valid", isError = true)
            binding.root.postDelayed({ finish() }, 700L)
            return
        }

        binding.tvIdInfo.text = "ID: $mahasiswaId"
        binding.etNama.setText(intent.getStringExtra(EXTRA_NAMA).orEmpty())
        binding.etNim.setText(intent.getStringExtra(EXTRA_NIM).orEmpty())

        binding.btnSimpan.setOnClickListener { update() }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun update() {
        val nama = binding.etNama.text.toString().trim()
        val nim = binding.etNim.text.toString().trim()
        val jurusanId = binding.etJurusanId.text.toString().trim()

        if (nama.isEmpty() || nim.isEmpty() || jurusanId.isEmpty()) {
            UiPopup.show(this, "Lengkapi nama, NIM, dan Jurusan ID", isError = true)
            return
        }

        if (nim.length < 8) {
            binding.etNim.error = "NIM minimal 8 karakter"
            return
        }

        if (jurusanId.toIntOrNull() == null) {
            binding.etJurusanId.error = "Jurusan ID harus angka"
            return
        }

        binding.btnSimpan.isEnabled = false
        binding.btnSimpan.text = "Mengupdate..."
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val body = mapOf(
                    "nama" to nama,
                    "nim" to nim,
                    "jurusan_id" to jurusanId
                )
                val response = withContext(Dispatchers.IO) {
                    ApiConfig.getApiService().updateMahasiswa(mahasiswaId, body)
                }

                if (response.isSuccessful && response.body()?.status == true) {
                    UiPopup.show(this@EditMahasiswaActivity, "Diupdate", isError = false)
                    setResult(RESULT_OK)
                    binding.root.postDelayed({ finish() }, 700L)
                } else {
                    val code = response.code()
                    val msg = if (response.body() != null) {
                        response.body()?.message ?: response.message()
                    } else {
                        parseErrorMessage(response)
                    }
                    UiPopup.show(
                        this@EditMahasiswaActivity,
                        "Gagal ($code): $msg",
                        isError = true,
                        durationMs = 3200L
                    )
                }
            } catch (e: SocketTimeoutException) {
                UiPopup.show(
                    this@EditMahasiswaActivity,
                    "Timeout: periksa IP & port",
                    isError = true,
                    durationMs = 3200L
                )
            } catch (e: IOException) {
                UiPopup.show(
                    this@EditMahasiswaActivity,
                    "Gagal terhubung (${e.localizedMessage})",
                    isError = true,
                    durationMs = 3200L
                )
            } catch (e: HttpException) {
                UiPopup.show(
                    this@EditMahasiswaActivity,
                    "Kesalahan HTTP (${e.code()})",
                    isError = true,
                    durationMs = 3200L
                )
            } catch (e: Exception) {
                UiPopup.show(
                    this@EditMahasiswaActivity,
                    "Terjadi kesalahan (${e.localizedMessage})",
                    isError = true,
                    durationMs = 3200L
                )
            } finally {
                binding.btnSimpan.isEnabled = true
                binding.btnSimpan.text = "UPDATE"
                binding.progressBar.visibility = View.GONE
            }
        }
    }
}