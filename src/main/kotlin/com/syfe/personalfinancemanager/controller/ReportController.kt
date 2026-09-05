package com.syfe.personalfinancemanager.controller

import com.syfe.personalfinancemanager.dto.MonthlyReportResponse
import com.syfe.personalfinancemanager.dto.YearlyReportResponse
import com.syfe.personalfinancemanager.security.SecurityUtils
import com.syfe.personalfinancemanager.service.ReportService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/reports")
class ReportController(
    private val reportService: ReportService
) {

    @GetMapping("/monthly/{year}/{month}")
    fun getMonthlyReport(
        @PathVariable year: Int,
        @PathVariable month: Int
    ): ResponseEntity<MonthlyReportResponse> {
        val userId = SecurityUtils.currentUserId()
        return ResponseEntity.ok(reportService.getMonthlyReport(userId, year, month))
    }

    @GetMapping("/yearly/{year}")
    fun getYearlyReport(@PathVariable year: Int): ResponseEntity<YearlyReportResponse> {
        val userId = SecurityUtils.currentUserId()
        return ResponseEntity.ok(reportService.getYearlyReport(userId, year))
    }
}
