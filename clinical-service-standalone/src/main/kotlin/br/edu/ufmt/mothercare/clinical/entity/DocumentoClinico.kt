package br.edu.ufmt.mothercare.clinical.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/**
 * UC07 - Gerenciar Prontuário Digital / RF07.
 *
 * Armazena o binário do laudo/exame diretamente no PostgreSQL (@Lob /
 * bytea), fiel à Seção 4.3.3 do TCC, que define apenas o PostgreSQL como
 * tecnologia de persistência — sem menção a object storage externo.
 *
 * Trade-off assumido conscientemente: bytea no Postgres escala pior que
 * um object storage dedicado (S3/MinIO) para arquivos grandes ou alto
 * volume, o que pode pressionar a meta de disponibilidade da RNF02 em
 * produção real. Ver README do módulo para a evolução sugerida.
 *
 * [tipoExameRastreado] só é preenchido quando a [categoria] corresponde
 * a um [TipoExame] que o sistema já processa numericamente (Hemoglobina,
 * Glicemia de Jejum) — nesse caso, o [ExameLaboratorial] correspondente
 * referencia este documento via [ExameLaboratorial.documentoId]. Para os
 * demais itens do checklist de exames (UC06), o documento existe sozinho,
 * sem entrada numérica associada.
 */
@Entity
@Table(name = "documentos_clinicos")
class DocumentoClinico(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(nullable = false)
    val gestanteId: UUID,

    @Column(nullable = false)
    val categoria: String,

    @Enumerated(EnumType.STRING)
    @Column
    val tipoExameRastreado: TipoExame? = null,

    @Column(nullable = false)
    val nomeArquivoOriginal: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val tipoConteudo: TipoConteudoArquivo,

    @Column(nullable = false)
    val tamanhoBytes: Long,

    @Lob
    @Column(nullable = false)
    val conteudo: ByteArray,

    @Column(nullable = false)
    val enviadoEm: Instant = Instant.now()
)
