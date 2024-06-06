package org.ripex.namelessgsnleash

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Bat
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.scheduler.BukkitRunnable

class NamelessGSNLeash : JavaPlugin(), Listener {

    private val playersLeashed = mutableSetOf<String>()
    private val leashCooldowns = HashMap<Player, Long>()

    // Метод, вызываемый при включении плагина
    override fun onEnable() {
        // Регистрация событий
        server.pluginManager.registerEvents(this, this)
        Bukkit.getLogger().info("NamelessGSNLeash enabled11.")
    }

    // Обработчик события взаимодействия игрока с сущностью
    @EventHandler
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val player = event.player
        val target = event.rightClicked



        if (player.inventory.itemInMainHand.type == Material.LEAD && target is Player && player.hasPermission("gsnleash.use")){

            // Проверка на время задержки между кликами
            val cooldown = leashCooldowns[player] ?: 0
            if (System.currentTimeMillis() - cooldown < 1000) {
                return // Если прошло менее секунды с предыдущего клика, игнорируем текущий клик
            }

            leashCooldowns[player] = System.currentTimeMillis()

            player.sendMessage(Component.text("Вы развязали: ${target.name}", NamedTextColor.GREEN))
            target.sendMessage(Component.text("Вас развязал: ${player.name}", NamedTextColor.GREEN))

            if(playersLeashed.contains(target.name)){
                val nearbyBats = player.location.world.getNearbyEntities(player.location,3.0,3.0,3.0)
                    .filterIsInstance<Bat>()
                    .filter { it.isLeashed && it.leashHolder == player }
                player.sendMessage("$nearbyBats")
                nearbyBats.forEach { it.remove() }
                playersLeashed.remove(target.name)
            }
            else{
                player.sendMessage(Component.text("Вы связали: ${target.name}", NamedTextColor.RED))
                target.sendMessage(Component.text("Вас связал: ${player.name}", NamedTextColor.RED))

                playersLeashed.add(target.name)

                val world = player.world

                // Создание видимой летучей мыши
                val bat = world.spawn(player.location, Bat::class.java) { entity ->
                    entity.isInvisible = true // Оставляем летучую мышь видимой для отладки
                    entity.isInvulnerable = true // Отключаем неуязвимость для отладки
                    entity.customName(Component.text("${target.name}", NamedTextColor.YELLOW)) // Сохраняем имя игрока, чтобы отличать летучих мышей
                    entity.setAI(false)
                    entity.setSilent(true)
                    entity.isCustomNameVisible = false
                }

                // Привязка летучей мыши к игроку
                bat.setLeashHolder(player)
                Bukkit.getLogger().info("Player ${player.name} has leashed a bat.")

                // Блок кода для перемещения летучей мыши в сторону игрока
                var task: BukkitRunnable? = null
                task = object : BukkitRunnable() {
                    override fun run() {
                        if (!bat.isLeashed || bat.leashHolder != player || bat.isDead || !target.isOnline || !player.isOnline) {
                            bat.remove()
                            playersLeashed.remove(target.name)
                            task?.cancel()
                            return
                        }

                        // Получаем вектор направления от летучей мыши к игроку
                        val direction = player.location.subtract(bat.location).toVector().normalize()

                        // Проверяем, если расстояние между летучей мышью и игроком менее 2 блоков, прекращаем перемещение
                        if(bat.location.distance(player.location) > 1.5){
                            bat.teleport(bat.location.add(direction))
                        }
                        if (bat.location.block.isSolid){
                            player.sendMessage("solid")
                            bat.teleport(bat.location.add(0.0,1.0,0.0))//ЗДЕСЬ НУЖНО БУДЕТ ПОМЕНЯТЬ 1.0 НА РАЗНИЦУ ЧТОБЫ МЫШЬ ОКАЗАЛАСЬ НА ВЕРХУШКЕ БЛОКА
                        }
                        target.teleport(bat.location.add(0.0, 0.0, 0.0))
                    }
                }
                task.runTaskTimer(this, 0L, 2L)  // Запуск задачи с периодом 5 тиков
            }
        }else {
            Bukkit.getLogger().info("${player.name} is not holding a lead or target is not a player.")
        }
    }
}
