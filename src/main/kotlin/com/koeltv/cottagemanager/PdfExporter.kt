package com.koeltv.cottagemanager

import com.lowagie.text.*
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import com.lowagie.text.pdf.PdfWriter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

/**
 * A very simple PdfPTable example.
 */
object PdfExporter {
    private val boldFont: Font = FontFactory.getFont(FontFactory.HELVETICA_BOLD)

    fun exportFormattedReservations(
        outputFile: File,
        reservations: SortedSet<Reservation>,
        censored: Boolean = false
    ) {
        println("Exporting to $outputFile...")

        val document = Document()

        try {
            PdfWriter.getInstance(document, FileOutputStream(outputFile))

            document.run {
                open()

                addTitle(reservations)

                var currentYear = reservations.first().arrivalDate.year

                for (reservation in reservations) {
                    if (reservation.arrivalDate.year > currentYear) {
                        addYearSumUp(reservations, currentYear)
                        add(Paragraph("\n"))
                        currentYear = reservation.arrivalDate.year
                    }
                    addFormattedReservation(pageSize.width, reservation, censored)
                }

                addYearSumUp(reservations, currentYear)
            }

            println("Export success !")
        } catch (de: DocumentException) {
            System.err.println(de.message)
        } catch (de: IOException) {
            System.err.println(de.message)
        }

        document.close()
    }

    private fun Document.addTitle(reservations: Collection<Reservation>) {
        val cottageAliases = reservations.map { it.cottage.alias }.distinct()
        val title = if (cottageAliases.size == 1) {
            cottageAliases.first()
        } else {
            "Tous les gîtes"
        }

        add(Paragraph(title).apply {
            alignment = Element.ALIGN_CENTER
            font.size += 25
        })
        add(Paragraph("\n"))
    }

    private fun Document.addYearSumUp(
        reservations: Collection<Reservation>,
        currentYear: Int
    ) {
        val yearlyReservations = reservations.filter { it.arrivalDate.year == currentYear }
        add(
            Paragraph(
                "$currentYear = ${yearlyReservations.size} locations = ${
                    yearlyReservations.sumOf { it.price }.toPriceString()
                }",
                boldFont
            )
        )
    }

    private fun Document.addFormattedReservation(
        width: Float,
        reservation: Reservation,
        censored: Boolean = false
    ) {
        val table = PdfPTable(6)
        table.horizontalAlignment = 0
        table.totalWidth = width - 72
        table.isLockedWidth = true
        table.keepTogether = true

        // Line 0 - Month and Year with no border
        val formattedMonth = reservation.arrivalDate.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.FRANCE)
        table.addCell(
            PdfPCell(
                Paragraph("${formattedMonth.uppercaseFirst()} ${reservation.arrivalDate.year}\n")
            ).apply {
                borderWidth = 0f
                colspan = 6
            }
        )
        // Line 1
        table.addCell(PdfPCell(
            Paragraph(
                "Du ${reservation.arrivalDate.format(DateTimeFormatter.ofPattern("dd/MM"))} au ${
                    reservation.departureDate.format(
                        DateTimeFormatter.ofPattern("dd/MM")
                    )
                }"
            )
        ).apply {
            colspan = 2
            horizontalAlignment = Element.ALIGN_CENTER
        })
        table.addCell(PdfPCell(Paragraph(reservation.note?.toPlusNote())).apply {
            colspan = 1
            horizontalAlignment = Element.ALIGN_CENTER
        })

        var personCountString = "${reservation.adultCount} adultes"
        if (reservation.childCount > 0u) personCountString += ", ${reservation.childCount} enfant"
        if (reservation.babyCount > 0u) personCountString += ", ${reservation.babyCount} BB"

        table.addCell(PdfPCell(Paragraph(personCountString)).apply {
            colspan = 3
            horizontalAlignment = Element.ALIGN_CENTER
        })
        // Line 2
        table.addCell(PdfPCell(Paragraph(reservation.client.name)).apply {
            colspan = 3
            horizontalAlignment = Element.ALIGN_CENTER
        })
        table.addCell(PdfPCell(Paragraph(reservation.confirmationCode)).apply {
            colspan = 3
            horizontalAlignment = Element.ALIGN_CENTER
        })
        // Line 3 (can be censored)
        if (!censored) {
            table.addCell(PdfPCell(Paragraph(reservation.client.phoneNumber)).apply {
                colspan = 3
                horizontalAlignment = Element.ALIGN_CENTER
            })
            table.addCell(PdfPCell(Paragraph(reservation.price.toPriceString())).apply {
                colspan = 3
                horizontalAlignment = Element.ALIGN_CENTER
            })
        }

        add(table)
        add(Paragraph("\n"))
    }
}