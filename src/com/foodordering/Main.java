package com.foodordering;

import java.sql.*;
import java.util.Scanner;

public class Main {

    static String url = "jdbc:mysql://localhost:3306/food_ordering";
    static String username = "root";
    static String password = "Muni@80088##";

    static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");

            Connection con = DriverManager.getConnection(url, username, password);

            System.out.println("Database Connected Successfully!");

            while (true) {

                System.out.println("\n===== ONLINE FOOD ORDERING SYSTEM =====");
                System.out.println("1. Customer Registration");
                System.out.println("2. View Menu");
                System.out.println("3. Place Order");
                System.out.println("4. Order History");
                System.out.println("5. Generate Bill");
                System.out.println("6. View Customers");
                System.out.println("7. Exit");

                System.out.print("Enter your choice: ");
                int choice = sc.nextInt();

                switch (choice) {

                case 1:
                    registerCustomer(con);
                    break;

                case 2:
                    viewMenu(con);
                    break;

                case 3:
                    placeOrder(con);
                    break;

                case 4:
                    orderHistory(con);
                    break;

                case 5:
                    generateBill(con);
                    break;

                case 6:
                    viewCustomers(con);
                    break;

                case 7:
                    System.out.println("Thank you!");
                    con.close();
                    sc.close();
                    System.exit(0);

                default:
                    System.out.println("Invalid choice!");
                }
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // 1. Customer Registration
    static void registerCustomer(Connection con) {

        try {

            sc.nextLine();

            System.out.print("Enter Customer Name: ");
            String name = sc.nextLine();

            System.out.print("Enter Email: ");
            String email = sc.nextLine();

            System.out.print("Enter Phone: ");
            String phone = sc.nextLine();

            String sql = "INSERT INTO customers(customer_name, email, phone) VALUES (?, ?, ?)";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, phone);

            ps.executeUpdate();

            System.out.println("Customer Registered Successfully!");

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // 2. View Menu
    static void viewMenu(Connection con) {

        try {

            String sql = "SELECT * FROM menu WHERE available = TRUE";

            Statement st = con.createStatement();

            ResultSet rs = st.executeQuery(sql);

            System.out.println("\n----- MENU -----");

            while (rs.next()) {

                System.out.println(
                    rs.getInt("item_id") + " | " +
                    rs.getString("item_name") + " | Rs." +
                    rs.getDouble("price")
                );
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // 3. Place Order
    static void placeOrder(Connection con) {

        try {

            System.out.print("Enter Customer ID: ");
            int customerId = sc.nextInt();

            viewMenu(con);

            System.out.print("Enter Item ID: ");
            int itemId = sc.nextInt();

            System.out.print("Enter Quantity: ");
            int quantity = sc.nextInt();

            // Get item price
            String priceSql = "SELECT price FROM menu WHERE item_id = ? AND available = TRUE";

            PreparedStatement pricePs = con.prepareStatement(priceSql);

            pricePs.setInt(1, itemId);

            ResultSet rs = pricePs.executeQuery();

            if (!rs.next()) {
                System.out.println("Food item not found!");
                return;
            }

            double price = rs.getDouble("price");

            double total = price * quantity;

            // Start Transaction
            con.setAutoCommit(false);

            // Insert order
            String orderSql =
                "INSERT INTO orders(customer_id, order_date, total_amount) VALUES (?, CURDATE(), ?)";

            PreparedStatement orderPs =
                con.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS);

            orderPs.setInt(1, customerId);
            orderPs.setDouble(2, total);

            orderPs.executeUpdate();

            ResultSet keys = orderPs.getGeneratedKeys();

            int orderId = 0;

            if (keys.next()) {
                orderId = keys.getInt(1);
            }

            // Insert order item
            String itemSql =
                "INSERT INTO order_items(order_id, item_id, quantity, price) VALUES (?, ?, ?, ?)";

            PreparedStatement itemPs = con.prepareStatement(itemSql);

            itemPs.setInt(1, orderId);
            itemPs.setInt(2, itemId);
            itemPs.setInt(3, quantity);
            itemPs.setDouble(4, price);

            itemPs.executeUpdate();

            // Commit transaction
            con.commit();

            con.setAutoCommit(true);

            System.out.println("\nOrder Placed Successfully!");
            System.out.println("Order ID: " + orderId);
            System.out.println("Total Amount: Rs." + total);

        } catch (Exception e) {

            try {
                con.rollback();
                con.setAutoCommit(true);
            } catch (SQLException ex) {
                System.out.println("Rollback Error: " + ex.getMessage());
            }

            System.out.println("Order Failed: " + e.getMessage());
        }
    }

    // 4. Order History
    static void orderHistory(Connection con) {

        try {

            System.out.print("Enter Customer ID: ");
            int customerId = sc.nextInt();

            String sql =
                "SELECT o.order_id, o.order_date, " +
                "m.item_name, oi.quantity, oi.price, o.total_amount " +
                "FROM orders o " +
                "JOIN order_items oi ON o.order_id = oi.order_id " +
                "JOIN menu m ON oi.item_id = m.item_id " +
                "WHERE o.customer_id = ?";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setInt(1, customerId);

            ResultSet rs = ps.executeQuery();

            System.out.println("\n----- ORDER HISTORY -----");

            while (rs.next()) {

                System.out.println(
                    "Order ID: " + rs.getInt("order_id") +
                    " | Date: " + rs.getDate("order_date") +
                    " | Item: " + rs.getString("item_name") +
                    " | Quantity: " + rs.getInt("quantity") +
                    " | Price: Rs." + rs.getDouble("price") +
                    " | Total: Rs." + rs.getDouble("total_amount")
                );
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // 5. Generate Bill
    static void generateBill(Connection con) {

        try {

            System.out.print("Enter Order ID: ");
            int orderId = sc.nextInt();

            String sql =
                "SELECT c.customer_name, o.order_id, o.order_date, " +
                "m.item_name, oi.quantity, oi.price, o.total_amount " +
                "FROM orders o " +
                "JOIN customers c ON o.customer_id = c.customer_id " +
                "JOIN order_items oi ON o.order_id = oi.order_id " +
                "JOIN menu m ON oi.item_id = m.item_id " +
                "WHERE o.order_id = ?";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setInt(1, orderId);

            ResultSet rs = ps.executeQuery();

            System.out.println("\n========== BILL ==========");

            boolean found = false;

            while (rs.next()) {

                found = true;

                System.out.println("Customer : " + rs.getString("customer_name"));
                System.out.println("Order ID : " + rs.getInt("order_id"));
                System.out.println("Date     : " + rs.getDate("order_date"));
                System.out.println("--------------------------");

                System.out.println(
                    rs.getString("item_name") +
                    " x " +
                    rs.getInt("quantity") +
                    " = Rs." +
                    (rs.getDouble("price") * rs.getInt("quantity"))
                );

                System.out.println("--------------------------");
                System.out.println("Total Amount: Rs." +
                        rs.getDouble("total_amount"));
            }

            if (!found) {
                System.out.println("Order not found!");
            }

            System.out.println("==========================");

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    // 6. View Customers
    static void viewCustomers(Connection con) {

        try {

            String sql = "SELECT * FROM customers";

            Statement st = con.createStatement();

            ResultSet rs = st.executeQuery(sql);

            System.out.println("\n----- CUSTOMERS -----");

            while (rs.next()) {

                System.out.println(
                    rs.getInt("customer_id") + " | " +
                    rs.getString("customer_name") + " | " +
                    rs.getString("email") + " | " +
                    rs.getString("phone")
                );
            }

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}