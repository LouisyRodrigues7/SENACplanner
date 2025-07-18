package com.example.pivot.Acoes

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.pivot.Acoes.Type.Usuario
import com.example.pivot.data.DatabaseHelper
import com.example.pivot.R
import com.example.pivot.adapter.TipoNotificacao
import java.text.SimpleDateFormat
import java.util.*

/**
 * Activity responsável pela criação de uma nova atividade vinculada a uma ação.
 *
 * Permite ao usuário inserir nome, descrição, datas de início e conclusão, selecionar status e responsável,
 * além de salvar a atividade no banco de dados. Também gerencia notificações para aprovação de atividades.
 */
class CriarAtividadeActivity : AppCompatActivity() {

    private lateinit var nomeEditText: EditText
    private lateinit var descricaoEditText: EditText
    private lateinit var dataInicioButton: Button
    private lateinit var dataConclusaoButton: Button
    private lateinit var statusSpinner: Spinner
    private lateinit var salvarButton: Button
    private lateinit var spinnerResponsavel: Spinner
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var cancelarButton: Button


    private var dataInicio: String = ""
    private var dataConclusao: String = ""
    private var acaoId: Int = -1
    private var usuarioId: Int = -1
    private var idUsuario: Int = -1
    private var usuarioTipo: String = ""
    private var pilarNome: String = ""
    private var atividadeId: Long = -1

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_criar_atividade)

        nomeEditText = findViewById(R.id.editNome)
        descricaoEditText = findViewById(R.id.editDescricao)
        dataInicioButton = findViewById(R.id.btnDataInicio)
        dataConclusaoButton = findViewById(R.id.btnDataConclusao)
        statusSpinner = findViewById(R.id.spinnerStatus)
        salvarButton = findViewById(R.id.btnSalvar)
        spinnerResponsavel = findViewById(R.id.spinnerResponsavel)

        dbHelper = DatabaseHelper(this)

        acaoId = intent.getIntExtra("ACAO_ID", -1)
        usuarioId = intent.getIntExtra("ID_USUARIO", -1)
        pilarNome = intent.getStringExtra("PILAR_NOME").toString()
        usuarioTipo = intent.getStringExtra("TIPO_USUARIO").toString()

        val tipoUsuario = dbHelper.obterUsuario(usuarioId)
        Log.d("USUARIO ID", (tipoUsuario?.tipo ?: '-').toString())
        val responsaveis = dbHelper.listarResponsaveis()
        val adapter = ArrayAdapter(this, R.layout.spinner_item, responsaveis)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerResponsavel.adapter = adapter

        if (acaoId == -1 || usuarioId == -1) {
            Toast.makeText(this, "Erro ao obter contexto da ação ou usuário.", Toast.LENGTH_SHORT)
                .show()
            finish()
        }

        statusSpinner.adapter = ArrayAdapter.createFromResource(
            this,
            R.array.status_array,
            android.R.layout.simple_spinner_item
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        dataInicioButton.setOnClickListener {
            showDatePicker { date ->
                dataInicio = date
                dataInicioButton.text = formatarDataParaBR(date)
            }
        }

        dataConclusaoButton.setOnClickListener {
            showDatePicker { date ->
                dataConclusao = date
                dataConclusaoButton.text = formatarDataParaBR(date)
            }
        }

        salvarButton.setOnClickListener {
            val nome = nomeEditText.text.toString()
            val descricao = descricaoEditText.text.toString()
            val status = statusSpinner.selectedItem.toString()
            val usuarioSelecionado = spinnerResponsavel.selectedItem as? Usuario

            if (nome.isBlank() || dataInicio.isBlank()) {
                Toast.makeText(this, "Preencha os campos obrigatórios.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (usuarioSelecionado == null) {
                Toast.makeText(this, "Selecione um responsável.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val aprovado = usuarioTipo == "Coordenador"

            atividadeId = dbHelper.inserirAtividade(
                acaoId,
                nome,
                descricao,
                status,
                dataInicio,
                dataConclusao,
                usuarioId,
                usuarioSelecionado.id,
                aprovado
            )

            if (!aprovado) {
                dbHelper.criarNotificacaoParaCoordenador(
                    "Nova atividade ($nome) aguardando aprovação!!",
                    atividadeId.toInt(),
                    null,
                    TipoNotificacao.APROVACAO_ATIVIDADE
                )
                Toast.makeText(this, "Atividade aguardando aprovação!", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Atividade criada e aprovada com sucesso!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        cancelarButton = findViewById(R.id.btnCancelar)
        
        cancelarButton.setOnClickListener {
            finish()  // Fecha a activity e volta para a tela anterior
        }


    }

    /**
     * Exibe um DatePickerDialog para seleção de data.
     * @param callback Função de callback que recebe a data selecionada no formato ISO (yyyy-MM-dd).
     */
    private fun showDatePicker(callback: (String) -> Unit) {
        val c = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            val data = Calendar.getInstance()
            data.set(year, month, day)
            callback(dateFormat.format(data.time))
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }


    fun formatarDataParaBR(dataISO: String): String {
        return try {
            val timestamp = dataISO.toLongOrNull()
            val data: Date? = if (timestamp != null) {
                Date(timestamp)
            } else {
                val formatoEntrada = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                formatoEntrada.parse(dataISO)
            }
            val formatoSaida = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            if (data != null) formatoSaida.format(data) else ""
        } catch (e: Exception) {
            ""
        }

    }
}