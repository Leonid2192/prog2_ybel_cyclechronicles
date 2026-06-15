package cyclechronicles;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ShopTest {

    private Shop shop;

    @BeforeEach
    void setUp() {
        // Vor jedem Test einen frischen, leeren Shop erstellen
        shop = new Shop();
    }

    @Test
    void testAccept_ValidBike() {
        // TF_01: Ein ganz normales Rennrad (RACE) abgeben
        Order mockOrder = mock(Order.class);

        when(mockOrder.getBicycleType()).thenReturn(Type.RACE);
        when(mockOrder.getCustomer()).thenReturn("Robin");

        boolean result = shop.accept(mockOrder);
        assertTrue(result, "Ein normales Rennrad sollte angenommen werden.");
    }

    @Test
    void testAccept_RejectEBike() {
        // TF_02: Ablehnung E-Bike (EBIKE)
        Order mockOrder = mock(Order.class);
        when(mockOrder.getBicycleType()).thenReturn(Type.EBIKE);
        when(mockOrder.getCustomer()).thenReturn("Fabio");

        boolean result = shop.accept(mockOrder);
        assertFalse(result, "E-Bikes duerfen nicht angenommen werden.");
    }

    @Test
    void testAccept_RejectGravelBike() {
        // TF_03: Ablehnung Gravel-Bike (GRAVEL)
        Order mockOrder = mock(Order.class);
        when(mockOrder.getBicycleType()).thenReturn(Type.GRAVEL);
        when(mockOrder.getCustomer()).thenReturn("Betuel");

        boolean result = shop.accept(mockOrder);
        assertFalse(result, "Gravel-Bikes duerfen nicht angenommen werden.");
    }

    @Test
    void testAccept_RejectCustomerWithExistingOrder() {
        // TF_04: Kunde hat bereits einen offenen Auftrag
        String customerName = "Joel";

        Order firstOrder = mock(Order.class);
        when(firstOrder.getBicycleType()).thenReturn(Type.RACE);
        when(firstOrder.getCustomer()).thenReturn(customerName);
        shop.accept(firstOrder);

        // Die zweite Order für denselben Kunden
        Order secondOrder = mock(Order.class);
        when(secondOrder.getBicycleType()).thenReturn(Type.SINGLE_SPEED);
        when(secondOrder.getCustomer()).thenReturn(customerName);

        boolean result = shop.accept(secondOrder);
        assertFalse(result, "Kunde darf nicht zwei Auftraege gleichzeitig offen haben.");
    }

    @Test
    void testAccept_Boundary_QueueHasFourOrders() {
        // TF_05: Grenzwert - Warteschlange hat exakt 4 Auftraege von unterschiedlichen Kunden
        for (int i = 0; i < 4; i++) {
            Order existingOrder = mock(Order.class);
            when(existingOrder.getBicycleType()).thenReturn(Type.SINGLE_SPEED);
            when(existingOrder.getCustomer()).thenReturn("Kunde_" + i);
            shop.accept(existingOrder);
        }

        // Der 5. Auftrag kommt von einem neuen Kunden rein
        Order fifthOrder = mock(Order.class);
        when(fifthOrder.getBicycleType()).thenReturn(Type.RACE);
        when(fifthOrder.getCustomer()).thenReturn("NeuerKunde");

        boolean result = shop.accept(fifthOrder);
        assertTrue(result, "Der fuenfte Auftrag sollte noch in die Queue passen.");
    }

    @Test
    void testAccept_Boundary_QueueIsFullWithFiveOrders() {
        // TF_06: Grenzwert - Warteschlange hat bereits 5 Auftraege
        for (int i = 0; i < 5; i++) {
            Order existingOrder = mock(Order.class);
            when(existingOrder.getBicycleType()).thenReturn(Type.SINGLE_SPEED);
            when(existingOrder.getCustomer()).thenReturn("Kunde_" + i);
            shop.accept(existingOrder);
        }

        // Der 6. Auftrag versucht reinzukommen
        Order sixthOrder = mock(Order.class);
        when(sixthOrder.getBicycleType()).thenReturn(Type.RACE);
        when(sixthOrder.getCustomer()).thenReturn("UeberflüssigerKunde");

        boolean result = shop.accept(sixthOrder);
        assertFalse(result, "Der sechste Auftrag muss wegen voller Queue abgelehnt werden.");
    }

    @Test
    void testRepair_SuccessfulFifo() {
        // TF_07: Nominalwert - Reparatur nach dem FIFO-Prinzip (First-In-First-Out)
        Order firstOrder = mock(Order.class);
        when(firstOrder.getBicycleType()).thenReturn(Type.RACE);
        when(firstOrder.getCustomer()).thenReturn("Kunde_A");

        Order secondOrder = mock(Order.class);
        when(secondOrder.getBicycleType()).thenReturn(Type.SINGLE_SPEED);
        when(secondOrder.getCustomer()).thenReturn("Kunde_B");

        // Beide Aufträge nacheinander hinzufügen
        shop.accept(firstOrder);
        shop.accept(secondOrder);

        // Die erste Reparatur muss den ältesten Auftrag (firstOrder / Kunde_A) liefern
        java.util.Optional<Order> repairedOrder = shop.repair();

        assertTrue(repairedOrder.isPresent(), "Es sollte ein reparariertes Fahrrad vorhanden sein.");
        assertEquals(firstOrder, repairedOrder.get(), "Nach FIFO muss der erste Auftrag zuerst repariert werden.");
    }

    @Test
    void testRepair_EmptyQueueReturnsEmptyOptional() {
        // TF_08: Fehlerpfad - Reparaturversuch bei komplett leerem Shop
        java.util.Optional<Order> repairedOrder = shop.repair();

        assertFalse(repairedOrder.isPresent(), "Bei leerer Warteschlange muss ein leeres Optional kommen.");
    }

    @Test
    void testDeliver_SuccessfulDeliveryAndRemoval() {
        // TF_09: Nominalwert - Erfolgreiche Auslieferung und anschließende Löschung
        Order mockOrder = mock(Order.class);
        when(mockOrder.getBicycleType()).thenReturn(Type.RACE);
        when(mockOrder.getCustomer()).thenReturn("Robin");

        // Auftrag annehmen und reparieren, damit er in den completedOrders landet
        shop.accept(mockOrder);
        shop.repair();

        // 1. Abholung: Muss die korrekte Order von "Robin" liefern
        java.util.Optional<Order> deliveredOrder = shop.deliver("Robin");
        assertTrue(deliveredOrder.isPresent(), "Die Order fuer Robin sollte abholbereit sein.");
        assertEquals(mockOrder, deliveredOrder.get());

        // 2. Abholung (Zustandsprüfung): Da die Order ausgehändigt wurde, muss sie jetzt weg sein
        java.util.Optional<Order> secondDelivery = shop.deliver("Robin");
        assertFalse(secondDelivery.isPresent(), "Das Fahrrad wurde bereits abgeholt und darf nicht mehr im Shop sein.");
    }

    @Test
    void testDeliver_UnknownCustomerReturnsEmptyOptional() {
        // TF_10: Fehlerpfad - Abholversuch für einen Kunden ohne fertigen Auftrag
        Order mockOrder = mock(Order.class);
        when(mockOrder.getBicycleType()).thenReturn(Type.RACE);
        when(mockOrder.getCustomer()).thenReturn("Fabio");

        // Fabios Rad annehmen und reparieren
        shop.accept(mockOrder);
        shop.repair();

        // Betuel versucht ein Rad abzuholen, obwohl nur eins für Fabio bereitsteht
        java.util.Optional<Order> deliveredOrder = shop.deliver("Betuel");

        assertFalse(deliveredOrder.isPresent(), "Fuer Betuel liegt kein repariertes Fahrrad vor.");
    }

}
