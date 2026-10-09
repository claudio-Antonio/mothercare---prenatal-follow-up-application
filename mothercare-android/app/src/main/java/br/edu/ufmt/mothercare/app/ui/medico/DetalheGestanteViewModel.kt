package br.edu.ufmt.mothercare.app.ui.medico

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.ufmt.mothercare.app.data.Resultado
import br.edu.ufmt.mothercare.app.data.remote.dto.*
import br.edu.ufmt.mothercare.app.data.repository.*
import br.edu.ufmt.mothercare.app.ui.components.Carregavel
import br.edu.ufmt.mothercare.app.ui.components.paraCarregavel
import br.edu.ufmt.mothercare.app.ui.prontuario.ArquivoParaAbrir
import br.edu.ufmt.mothercare.app.ui.prontuario.mimeTypeDoDocumento
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Cada seção carrega e falha de forma independente: um laudo indisponível não esconde a PA. */
data class DetalheGestanteUiState(
    val idade: Carregavel<IdadeGestacionalResponse> = Carregavel(),
    val pressao: Carregavel<List<RegistroPressaoResponse>> = Carregavel(),
    val consultas: Carregavel<List<ConsultaResponse>> = Carregavel(),
    val checklist: Carregavel<ChecklistExamesResponse> = Carregavel(),
    val exames: Carregavel<List<ExameHistoricoResponse>> = Carregavel(),
    val peso: Carregavel<GraficoPesoResponse> = Carregavel(),
    val documentos: Carregavel<List<DocumentoClinicoResponse>> = Carregavel(),
    val baixandoId: String? = null,
    val erroArquivo: String? = null,
    val arquivoParaAbrir: ArquivoParaAbrir? = null
)

/**
 * UC07 - "O médico visualiza os dados no painel clínico". Só leitura: o
 * clinical-service recusa (403) escritas do médico em nome da gestante.
 */
class DetalheGestanteViewModel(
    private val gestanteId: String,
    private val gestanteRepository: GestanteRepository,
    private val checkInRepository: CheckInRepository,
    private val agendamentoRepository: AgendamentoRepository,
    private val checklistRepository: ChecklistRepository,
    private val exameRepository: ExameRepository,
    private val pesoRepository: PesoRepository,
    private val prontuarioRepository: ProntuarioRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetalheGestanteUiState())
    val uiState: StateFlow<DetalheGestanteUiState> = _uiState.asStateFlow()

    init { carregar() }

    fun carregar() {
        _uiState.value = DetalheGestanteUiState()
        viewModelScope.launch {
            val idade = async { gestanteRepository.obterIdadeGestacional(gestanteId) }
            val pressao = async { checkInRepository.historico(gestanteId) }
            val consultas = async { agendamentoRepository.listar(gestanteId) }
            val checklist = async { checklistRepository.obterChecklist(gestanteId) }
            val exames = async { exameRepository.historico(gestanteId) }
            val peso = async { pesoRepository.obterGrafico(gestanteId) }
            val documentos = async { prontuarioRepository.listar(gestanteId) }
            _uiState.value = _uiState.value.copy(
                idade = idade.await().paraCarregavel(),
                pressao = pressao.await().paraCarregavel(),
                consultas = consultas.await().paraCarregavel(),
                checklist = checklist.await().paraCarregavel(),
                exames = exames.await().paraCarregavel(),
                peso = peso.await().paraCarregavel(),
                documentos = documentos.await().paraCarregavel()
            )
        }
    }

    fun abrirDocumento(documento: DocumentoClinicoResponse) {
        if (_uiState.value.baixandoId != null) return
        _uiState.value = _uiState.value.copy(baixandoId = documento.id, erroArquivo = null)
        viewModelScope.launch {
            _uiState.value = when (val resultado = prontuarioRepository.baixar(documento)) {
                is Resultado.Sucesso -> _uiState.value.copy(
                    baixandoId = null, arquivoParaAbrir = ArquivoParaAbrir(resultado.dados, mimeTypeDoDocumento(documento.tipoConteudo))
                )
                is Resultado.Erro -> _uiState.value.copy(baixandoId = null, erroArquivo = resultado.mensagem)
            }
        }
    }

    fun arquivoAberto() { _uiState.value = _uiState.value.copy(arquivoParaAbrir = null) }
    fun informarErroArquivo(mensagem: String) { _uiState.value = _uiState.value.copy(erroArquivo = mensagem) }
}
