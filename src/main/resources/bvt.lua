---@meta

---@class ContainerField
---@field children fun(): fun(): string Returns an iterator for the child field names

---@alias NamespacedKey string A key with an optional namespace

---Represents an item's lore.
---
---Note that trailing empty lines will be stripped.
---
---Due to Lua constraints, you cannot assign a string to this field. Instead, use the text field.
---@class Lore: ContainerField
---@field size integer
---@field new_line string A new line of text to append. Supports newlines.
---@field text string The lore of the item. Supports newlines.
---@field lines fun(): fun(): integer, string
---@field [integer] string A line of text.

---Represents a stack of item.
---@class ItemStack: ContainerField
---@field amount integer
---@field type NamespacedKey
---@field enchantments ItemEnchantments Deprecated. Use `safe_enchantments` or `unsafe_enchantments` instead.
---@field safe_enchantments ItemEnchantments
---@field unsafe_enchantments ItemEnchantments
---@field damage integer
---@field name string
---@field unbreakable boolean
---@field lore Lore

---The enchantments of an item
---@class ItemEnchantments: ContainerField
---@field size integer
---@field aqua_affinity integer
---@field bane_of_arthropods integer
---@field blast_protection integer
---@field channeling integer
---@field binding_curse integer
---@field vanishing_curse integer
---@field depth_strider integer
---@field efficiency integer
---@field feather_falling integer
---@field fire_aspect integer
---@field fire_protection integer
---@field flame integer
---@field fortune integer
---@field frost_walker integer
---@field impaling integer
---@field infinity integer
---@field knockback integer
---@field looting integer
---@field loyalty integer
---@field luck_of_the_sea integer
---@field lure integer
---@field mending integer
---@field multishot integer
---@field piercing integer
---@field power integer
---@field projectile_protection integer
---@field protection integer
---@field punch integer
---@field quick_charge integer
---@field respiration integer
---@field riptide integer
---@field sharpness integer
---@field silk_touch integer
---@field smite integer
---@field soul_speed integer
---@field sweeping integer
---@field swift_sneak integer
---@field thorns integer
---@field unbreaking integer
---@field [Enchantment] integer
---@field entries fun(): fun(): Enchantment, integer Returns an iterator of the enchantments on this item and their level

---@alias Enchantment
---| '"aqua_affinity"'
---| '"minecraft:aqua_affinity"'
---| '"bane_of_arthropods"'
---| '"minecraft:bane_of_arthropods"'
---| '"blast_protection"'
---| '"minecraft:blast_protection"'
---| '"channeling"'
---| '"minecraft:channeling"'
---| '"binding_curse"'
---| '"minecraft:binding_curse"'
---| '"vanishing_curse"'
---| '"minecraft:vanishing_curse"'
---| '"depth_strider"'
---| '"minecraft:depth_strider"'
---| '"efficiency"'
---| '"minecraft:efficiency"'
---| '"feather_falling"'
---| '"minecraft:feather_falling"'
---| '"fire_aspect"'
---| '"minecraft:fire_aspect"'
---| '"fire_protection"'
---| '"minecraft:fire_protection"'
---| '"flame"'
---| '"minecraft:flame"'
---| '"fortune"'
---| '"minecraft:fortune"'
---| '"frost_walker"'
---| '"minecraft:frost_walker"'
---| '"impaling"'
---| '"minecraft:impaling"'
---| '"infinity"'
---| '"minecraft:infinity"'
---| '"knockback"'
---| '"minecraft:knockback"'
---| '"looting"'
---| '"minecraft:looting"'
---| '"loyalty"'
---| '"minecraft:loyalty"'
---| '"luck_of_the_sea"'
---| '"minecraft:luck_of_the_sea"'
---| '"lure"'
---| '"minecraft:lure"'
---| '"mending"'
---| '"minecraft:mending"'
---| '"multishot"'
---| '"minecraft:multishot"'
---| '"piercing"'
---| '"minecraft:piercing"'
---| '"power"'
---| '"minecraft:power"'
---| '"projectile_protection"'
---| '"minecraft:projectile_protection"'
---| '"protection"'
---| '"minecraft:protection"'
---| '"punch"'
---| '"minecraft:punch"'
---| '"quick_charge"'
---| '"minecraft:quick_charge"'
---| '"respiration"'
---| '"minecraft:respiration"'
---| '"riptide"'
---| '"minecraft:riptide"'
---| '"sharpness"'
---| '"minecraft:sharpness"'
---| '"silk_touch"'
---| '"minecraft:silk_touch"'
---| '"smite"'
---| '"minecraft:smite"'
---| '"soul_speed"'
---| '"minecraft:soul_speed"'
---| '"sweeping"'
---| '"minecraft:sweeping"'
---| '"swift_sneak"'
---| '"minecraft:swift_sneak"'
---| '"thorns"'
---| '"minecraft:thorns"'
---| '"unbreaking"'
---| '"minecraft:unbreaking"'
---| string

---@class Trade: ContainerField
---@field apply_discounts boolean
---@field max_uses integer
---@field uses integer
---@field award_experience boolean
---@field villager_experience integer
---@field ingredient_0 ItemStack
---@field ingredient_1 ItemStack
---@field result ItemStack
---@field villager Villager
---@field index integer
---@field is_new boolean
---@field world World
trade = {}

---Represents a world.
---*Note: All fields are read-only*
---@class World: ContainerField
---@field name string
---@field environment "NORMAL"|"NETHER"|"THE_END"|"CUSTOM"
---@field time integer
---@field full_time integer
---@field is_day_time boolean
---@field weather "THUNDER"|"RAIN"|"CLEAR"
---@field seed integer The lower 32 bits of the world seed.
---@field seed_upper integer The upper 32 bits of the world seed.

---Represents a villager.
---@class Villager: ContainerField
---@field type "DESERT"|"JUNGLE"|"PLAINS"|"SAVANNA"|"SNOW"|"SWAMP"|"TAIGA"
---@field profession "NONE"|"ARMORER"|"BUTCHER"|"CARTOGRAPHER"|"CLERIC"|"FARMER"|"FISHERMAN"|"FLETCHER"|"LEATHERWORKER"|"LIBRARIAN"|"MASON"|"NITWIT"|"SHEPHERD"|"TOOLSMITH"|"WEAPONSMITH"
---@field level integer
---@field experience integer
---@field recipe_count integer
---@field world World
---@field inventory Inventory

---Represents the inventory of a villager.
---@class Inventory: ContainerField
---@field size integer
---@field empty boolean
---@field [0|1|2|3|4|5|6|7|8] ItemStack The item in the specified slot.
---@field [NamespacedKey] integer The number of items matching a type. 

---Utilities related to enchantments.
---@class EnchantmentUtils
---@field is_treasure fun(enchantment: Enchantment): boolean
enchantments = {
    ---@param level integer
    ---@param is_treasure boolean
    ---@return integer fromLevel
    ---@return integer toLevel
    get_cost=function(level, is_treasure) end
}

---Utilities related to color codes
---@class Color
color = {
    black='§0',
    dark_blue='§1',
    dark_green='§2',
    dark_aqua='§3',
    dark_red='§4',
    dark_purple='§5',
    gold='§6',
    gray='§7',
    dark_gray='§8',
    blue='§9',
    green='§a',
    aqua='§b',
    red='§c',
    light_purple='§d',
    yellow='§e',
    white='§f',
    obfuscated='§k',
    bold='§l',
    strikethrough='§m',
    underline='§n',
    italic='§o',
    reset='§r',
    ---@param r integer The red component, or an RGB integer, or a color string
    ---@param g integer The green component
    ---@param b integer The blue component
    ---@return string color
    ---@overload fun(rgb: integer): string
    ---@overload fun(color: string): string
    of=function(r, g, b) end
}