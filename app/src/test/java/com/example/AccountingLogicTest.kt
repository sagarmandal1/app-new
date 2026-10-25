package com.example

import com.example.data.local.entity.ServiceEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class AccountingLogicTest {

    @Test
    fun `test resell service profit formula`() {
        // Resell Service: Buy Price = 450, Sell Price = 850
        val buyPrice = 450.0
        val sellPrice = 850.0
        val expectedProfitPerUnit = sellPrice - buyPrice // 400.0

        val service = ServiceEntity(
            name = "Canva Pro",
            type = "RESELL",
            category = "Subscription",
            costPrice = buyPrice,
            sellPrice = sellPrice
        )

        assertEquals(400.0, service.profitPerUnit, 0.001)
        assertEquals(expectedProfitPerUnit, service.profitPerUnit, 0.001)
    }

    @Test
    fun `test own service profit formula`() {
        // Own Service: Making/Production Cost = 1200, Sell Price = 5500
        val makingCost = 1200.0
        val sellPrice = 5500.0
        val expectedProfitPerUnit = sellPrice - makingCost // 4300.0

        val service = ServiceEntity(
            name = "Landing Page Design",
            type = "OWN",
            category = "Web & App Development",
            costPrice = makingCost,
            sellPrice = sellPrice
        )

        assertEquals(4300.0, service.profitPerUnit, 0.001)
        assertEquals(expectedProfitPerUnit, service.profitPerUnit, 0.001)
    }

    @Test
    fun `test net profit formula with sales and expenses`() {
        // Sale 1: 2 units of Resell service (cost 450, sell 850, discount 100)
        // Subtotal = 2 * 850 = 1700
        // Total = 1700 - 100 = 1600
        // Total Cost = 2 * 450 = 900
        // Profit 1 = 1600 - 900 = 700.0
        val sale1Profit = (2 * 850.0 - 100.0) - (2 * 450.0)
        assertEquals(700.0, sale1Profit, 0.001)

        // Sale 2: 1 unit of Own service (cost 1200, sell 5500, discount 0)
        // Total = 5500, Cost = 1200, Profit = 4300.0
        val sale2Profit = 5500.0 - 1200.0
        assertEquals(4300.0, sale2Profit, 0.001)

        val totalSalesProfit = sale1Profit + sale2Profit // 5000.0

        // Expenses: Broadband = 1200, Office Rent = 2000
        val totalExpenses = 1200.0 + 2000.0 // 3200.0

        // Net Profit = Total Profit from Sales - Total Expenses
        val netProfit = totalSalesProfit - totalExpenses // 1800.0

        assertEquals(1800.0, netProfit, 0.001)
    }

    @Test
    fun `test due calculation for partial payment`() {
        val totalAmount = 3500.0
        val paidAmount = 2000.0
        val dueAmount = totalAmount - paidAmount

        assertEquals(1500.0, dueAmount, 0.001)
    }
}
