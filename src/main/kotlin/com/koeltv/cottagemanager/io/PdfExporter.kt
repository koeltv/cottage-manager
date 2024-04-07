package com.koeltv.cottagemanager.io

import com.koeltv.cottagemanager.data.Reservation
import com.koeltv.cottagemanager.toPlusNote
import com.koeltv.cottagemanager.toPriceString
import com.koeltv.cottagemanager.uppercaseFirst
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
    private val dayMonthDateFormatter = DateTimeFormatter.ofPattern("dd/MM")

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
                        addYearSumUp(reservations, currentYear, censored)
                        add(Paragraph("\n"))
                        currentYear = reservation.arrivalDate.year
                    }
                    addFormattedReservation(pageSize.width, reservation, censored)
                }

                addYearSumUp(reservations, currentYear, censored)
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
        currentYear: Int,
        censored: Boolean = false
    ) {
        val yearlyReservations = reservations.filter { it.arrivalDate.year == currentYear }
        var sumUp = "$currentYear = ${yearlyReservations.size} locations"
        if (!censored) {
            sumUp += " = ${yearlyReservations.sumOf { it.price }.toUInt().toPriceString()}"
        }
        add(Paragraph(sumUp, boldFont))
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
        table.addCenteredTextCell("${formattedMonth.uppercaseFirst()} ${reservation.arrivalDate.year}\n", colspan = 6)
        // Line 1
        table.addCenteredTextCell(
            "Du ${reservation.arrivalDate.format(dayMonthDateFormatter)} au ${
                reservation.departureDate.format(dayMonthDateFormatter)
            }",
            colspan = 2
        )
        table.addCenteredTextCell(reservation.note?.toUByte()?.toPlusNote() ?: "", colspan = 1)

        var personCountString = "${reservation.adultCount} adultes"
        if (reservation.childCount > 0) personCountString += ", ${reservation.childCount} enfant"
        if (reservation.babyCount > 0) personCountString += ", ${reservation.babyCount} BB"

        table.addCenteredTextCell(personCountString, colspan = 3)
        // Line 2
        table.addCenteredTextCell(reservation.client.name, colspan = 3)
        table.addCenteredTextCell(reservation.code, colspan = 3)
        // Line 3 (can be censored)
        if (!censored) {
            table.addCenteredTextCell(reservation.client.phoneNumber, colspan = 3)
            table.addCenteredTextCell(reservation.price.toUInt().toPriceString(), colspan = 3)
        }

        add(table)
        add(Paragraph("\n"))
    }

    private fun PdfPTable.addCenteredTextCell(text: String?, colspan: Int): PdfPCell {
        return addCell(PdfPCell(Paragraph(text)).apply {
            this.colspan = colspan
            this.horizontalAlignment = Element.ALIGN_CENTER
        })
    }
}