package com.dathuynh.simpleskyblock.commands;

import com.dathuynh.simpleskyblock.managers.AuthManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AuthCommand implements CommandExecutor {

    private AuthManager authManager;
    private String commandType; // "register", "login", "changepassword"

    public AuthCommand(AuthManager authManager, String commandType) {
        this.authManager = authManager;
        this.commandType = commandType;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cChỉ player mới có thể sử dụng lệnh này!");
            return true;
        }

        Player player = (Player) sender;

        switch (commandType) {
            case "register":
                return handleRegister(player, args);
            case "login":
                return handleLogin(player, args);
            case "changepassword":
                return handleChangePassword(player, args);
            default:
                return false;
        }
    }

    private boolean handleRegister(Player player, String[] args) {
        // Check đã login chưa
        if (authManager.isLoggedIn(player.getUniqueId())) {
            player.sendMessage("§cBạn đã đăng nhập rồi!");
            return true;
        }

        // Check đã đăng ký chưa
        if (authManager.isRegistered(player.getUniqueId())) {
            player.sendMessage("§cBạn đã đăng ký rồi! Sử dụng §e/login <password>");
            return true;
        }

        // Check arguments
        if (args.length != 2) {
            player.sendMessage("§e═══════════════════════════════");
            player.sendMessage("§6Cách sử dụng:");
            player.sendMessage("§7  /register <password> <password>");
            player.sendMessage("§e═══════════════════════════════");
            return true;
        }

        String password = args[0];
        String confirmPassword = args[1];

        // Validate password length
        if (password.length() < 6) {
            player.sendMessage("§cMật khẩu phải có ít nhất 6 ký tự!");
            return true;
        }

        // Check passwords match
        if (!password.equals(confirmPassword)) {
            player.sendMessage("§cMật khẩu xác nhận không khớp!");
            return true;
        }

        // Register
        if (authManager.register(player, password)) {
            // Auto login sau khi register
            authManager.login(player, password);

            player.sendMessage("§a═══════════════════════════════");
            player.sendMessage("§a Đăng ký thành công!");
            player.sendMessage("§7  Mật khẩu của bạn đã được mã hóa an toàn.");
            player.sendMessage("§7  Sử dụng §e/login <password> §7để đăng nhập.");
            player.sendMessage("§a═══════════════════════════════");
            return true;
        } else {
            player.sendMessage("§cĐã có lỗi xảy ra khi đăng ký!");
            return true;
        }
    }

    private boolean handleLogin(Player player, String[] args) {
        // Check đã login chưa
        if (authManager.isLoggedIn(player.getUniqueId())) {
            player.sendMessage("§cBạn đã đăng nhập rồi!");
            return true;
        }

        // Check đã đăng ký chưa
        if (!authManager.isRegistered(player.getUniqueId())) {
            player.sendMessage("§cBạn chưa đăng ký! Sử dụng §e/register <password> <password>");
            return true;
        }

        // Check arguments
        if (args.length != 1) {
            player.sendMessage("§e═══════════════════════════════");
            player.sendMessage("§6Cách sử dụng:");
            player.sendMessage("§7  /login <password>");
            player.sendMessage("§e═══════════════════════════════");
            return true;
        }

        String password = args[0];

        // Login
        if (authManager.login(player, password)) {
            player.sendMessage("§a═══════════════════════════════");
            player.sendMessage("§a  Đăng nhập thành công!");
            player.sendMessage("§7  Chào mừng trở lại!");
            player.sendMessage("§a═══════════════════════════════");
            return true;
        } else {
            player.sendMessage("§cMật khẩu không đúng!");
            return true;
        }
    }

    private boolean handleChangePassword(Player player, String[] args) {
        // Check đã login chưa
        if (!authManager.isLoggedIn(player.getUniqueId())) {
            player.sendMessage("§cBạn phải đăng nhập trước!");
            return true;
        }

        // Check arguments
        if (args.length != 2) {
            player.sendMessage("§e═══════════════════════════════");
            player.sendMessage("§6Cách sử dụng:");
            player.sendMessage("§7  /changepassword <oldPassword> <newPassword>");
            player.sendMessage("§e═══════════════════════════════");
            return true;
        }

        String oldPassword = args[0];
        String newPassword = args[1];

        // Validate new password length
        if (newPassword.length() < 6) {
            player.sendMessage("§cMật khẩu mới phải có ít nhất 6 ký tự!");
            return true;
        }

        // Change password
        if (authManager.changePassword(player, oldPassword, newPassword)) {
            player.sendMessage("§aĐổi mật khẩu thành công!");
            return true;
        } else {
            player.sendMessage("§cMật khẩu cũ không đúng!");
            return true;
        }
    }
}
