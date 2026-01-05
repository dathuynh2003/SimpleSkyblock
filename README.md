# 🏝️ SimpleSkyblock Plugin

A custom Minecraft Skyblock plugin built for Paper 1.20.4, providing essential skyblock mechanics and features for a complete skyblock experience.

## 📋 Overview

SimpleSkyblock is a lightweight, custom-built skyblock plugin that powers the [Minecraft Paper Skyblock Server](https://hub.docker.com/r/dathuynh2k3/minecraft-skyblock). This plugin provides core skyblock functionality including island management, player progression, and skyblock-specific game mechanics.

## ✨ Features

- **Island Management**: Create and manage personal skyblock islands
- **Player Progression**: Track player achievements and progress
- **Custom Mechanics**: Tailored gameplay mechanics for skyblock survival
- **Lightweight**:  Optimized for performance with minimal overhead
- **Paper 1.20.4**:  Built specifically for Paper server implementation

## 🚀 Quick Start

### Using Docker (Recommended)

The easiest way to use SimpleSkyblock is through the pre-configured Docker image:

```bash
docker run -d -p 25565:25565 \
  -v ./world:/minecraft/world \
  -v ./world_nether:/minecraft/world_nether \
  -v ./world_the_end:/minecraft/world_the_end \
  -v ./logs:/minecraft/logs \
  -v ./plugins:/minecraft/plugins \
  -e MEMORY=4G \
  --name minecraft-skyblock \
  --restart unless-stopped \
  dathuynh2k3/minecraft-skyblock:latest
```

**📦 Docker Hub**:  [dathuynh2k3/minecraft-skyblock](https://hub.docker.com/r/dathuynh2k3/minecraft-skyblock)

For detailed deployment instructions, server configuration, and management commands, visit the Docker Hub repository.

### Manual Installation

1. Download the latest `SimpleSkyblock-v1.0.jar` from releases
2. Place the JAR file in your Paper server's `plugins` folder
3. Restart your server
4. Configure the plugin in `plugins/SimpleSkyblock/config. yml`

## 🔧 Dependencies

This plugin works best with the following complementary plugins:

- **WorldEdit** v7.3.9 - For advanced world editing capabilities
- **VoidWorldGenerator** v1.3.11 - Generates void worlds for skyblock islands

## 📁 Project Structure

```
SimpleSkyblock/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/dathuynh/simpleskyblock/
│       │       ├── SimpleSkyblock.java       # Main plugin class
│       │       ├── commands/                 # Command handlers
│       │       ├── listeners/                # Event listeners
│       │       ├── managers/                 # Island/player managers
│       │       └── utils/                    # Utility classes
│       └── resources/
│           ├── plugin.yml                    # Plugin metadata
│           └── config.yml                    # Default configuration
├── pom.xml                                   # Maven configuration
└── README.md
```

## 🛠️ Building from Source

### Prerequisites

- Java 21 JDK
- Maven 3.6+
- Git

### Build Steps

```bash
# Clone the repository
git clone https://github.com/dathuynh2003/SimpleSkyblock.git
cd SimpleSkyblock

# Build with Maven
mvn clean package

# The compiled JAR will be in target/SimpleSkyblock-v1.0.jar
```

## ⚙️ Configuration

After first run, edit `plugins/SimpleSkyblock/config.yml`:

```yaml
# Island Settings
island: 
  default-size: 100
  spawn-protection: true
  max-islands-per-player: 1

# Player Settings
player: 
  starting-items:
    - GRASS_BLOCK: 1
    - ICE:2
    - LAVA_BUCKET:1
  
# World Settings
world:
  void-world:  true
  spawn-mobs: true
```

## 📖 Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/island` | Main island command | `simpleskyblock.island` |
| `/island create` | Create a new island | `simpleskyblock.island.create` |
| `/island home` | Teleport to your island | `simpleskyblock.island.home` |
| `/island delete` | Delete your island | `simpleskyblock.island.delete` |
| `/island invite <player>` | Invite a player to your island | `simpleskyblock.island.invite` |

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 Development Roadmap

- [ ] Team island support
- [ ] Island challenges and achievements
- [ ] Economy integration
- [ ] Custom island biomes
- [ ] Island leveling system
- [ ] GUI-based island management

## 🐛 Bug Reports

Found a bug? Please open an issue with: 
- Minecraft version
- Paper build number
- Plugin version
- Steps to reproduce
- Expected vs actual behavior

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🔗 Links

- **Docker Hub**: [dathuynh2k3/minecraft-skyblock](https://hub.docker.com/r/dathuynh2k3/minecraft-skyblock)
- **GitHub**: [SimpleSkyblock Repository](https://github.com/dathuynh2003/SimpleSkyblock)
- **Paper MC**: [Paper Documentation](https://docs.papermc.io/)

## 💬 Support

- Open an [Issue](https://github.com/dathuynh2003/SimpleSkyblock/issues) for bug reports
- Discussions for questions and feature requests

## 👨‍💻 Author

**Dat Huynh** - [@dathuynh2003](https://github.com/dathuynh2003)

---

⭐ If you find this plugin useful, please consider giving it a star! 

**Ready to play? ** Deploy the full server with Docker: 
```bash
docker pull dathuynh2k3/minecraft-skyblock:latest
```
