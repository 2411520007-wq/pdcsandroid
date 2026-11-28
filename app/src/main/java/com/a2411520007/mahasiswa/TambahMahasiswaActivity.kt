package com.a2411520007.mahasiswa

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.a2411520007.mahasiswa.api.ApiConfig
import com.a2411520007.mahasiswa.databinding.ActivityTambahMahasiswaBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

class TambahMahasiswaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTambahMahasiswaBinding

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
        binding = ActivityTambahMahasiswaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Tambah Mahasiswa"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.btnSimpan.setOnClickListener { simpan() }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun simpan() {
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
        binding.btnSimpan.text = "Menyimpan..."
        binding.progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val body = mapOf(
                    "nama" to nama,
                    "nim" to nim,
                    "jurusan_id" to jurusanId
                )
                val response = withContext(Dispatchers.IO) {
                    ApiConfig.getApiService().addMahasiswa(body)
                }

                if (response.isSuccessful && response.body()?.status == true) {
                    UiPopup.show(this@TambahMahasiswaActivity, "Tersimpan", isError = false)
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
                        this@TambahMahasiswaActivity,
                        "Gagal ($code): $msg",
                        isError = true,
                        durationMs = 3200L
                    )
                }
            } catch (e: SocketTimeoutException) {
                UiPopup.show(
                    this@TambahMahasiswaActivity,
                    "Timeout: periksa IP & port",
                    isError = true,
                    durationMs = 3200L
                )
            } catch (e: IOException) {
                UiPopup.show(
                    this@TambahMahasiswaActivity,
                    "Gagal terhubung (${e.localizedMessage})",
                    isError = true,
                    durationMs = 3200L
                )
            } catch (e: HttpException) {
                UiPopup.show(
                    this@TambahMahasiswaActivity,
                    "Kesalahan HTTP (${e.code()})",
                    isError = true,
                    durationMs = 3200L
                )
            } catch (e: Exception) {
                UiPopup.show(
                    this@TambahMahasiswaActivity,
                    "Terjadi kesalahan (${e.localizedMessage})",
                    isError = true,
                    durationMs = 3200L
                )
            } finally {
                binding.btnSimpan.isEnabled = true
                binding.btnSimpan.text = "SIMPAN"
                binding.progressBar.visibility = View.GONE
            }
        }
    }
}