package com.example.wirelesstransferandroid.internetsocket.cmd

enum class VirtualKeyCode(val i: Int) {
    //
    // 摘要:
    //     0 key
    VK_0(7),
    //
    // 摘要:
    //     1 key
    VK_1(8),
    //
    // 摘要:
    //     2 key
    VK_2(9),
    //
    // 摘要:
    //     3 key
    VK_3(10),
    //
    // 摘要:
    //     4 key
    VK_4(11),
    //
    // 摘要:
    //     5 key
    VK_5(12),
    //
    // 摘要:
    //     6 key
    VK_6(13),
    //
    // 摘要:
    //     7 key
    VK_7(14),
    //
    // 摘要:
    //     8 key
    VK_8(15),
    //
    // 摘要:
    //     9 key
    VK_9(16),
    //
    // 摘要:
    //     A key
    VK_A(29),
    //
    // 摘要:
    //     B key
    VK_B(30),
    //
    // 摘要:
    //     C key
    VK_C(31),
    //
    // 摘要:
    //     D key
    VK_D(32),
    //
    // 摘要:
    //     E key
    VK_E(33),
    //
    // 摘要:
    //     F key
    VK_F(34),
    //
    // 摘要:
    //     G key
    VK_G(35),
    //
    // 摘要:
    //     H key
    VK_H(36),
    //
    // 摘要:
    //     I key
    VK_I(37),
    //
    // 摘要:
    //     J key
    VK_J(38),
    //
    // 摘要:
    //     K key
    VK_K(39),
    //
    // 摘要:
    //     L key
    VK_L(40),
    //
    // 摘要:
    //     M key
    VK_M(41),
    //
    // 摘要:
    //     N key
    VK_N(42),
    //
    // 摘要:
    //     O key
    VK_O(43),
    //
    // 摘要:
    //     P key
    VK_P(44),
    //
    // 摘要:
    //     Q key
    VK_Q(45),
    //
    // 摘要:
    //     R key
    VK_R(46),
    //
    // 摘要:
    //     S key
    VK_S(47),
    //
    // 摘要:
    //     T key
    VK_T(48),
    //
    // 摘要:
    //     U key
    VK_U(49),
    //
    // 摘要:
    //     V key
    VK_V(50),
    //
    // 摘要:
    //     W key
    VK_W(51),
    //
    // 摘要:
    //     X key
    VK_X(52),
    //
    // 摘要:
    //     Y key
    VK_Y(53),
    //
    // 摘要:
    //     Z key
    VK_Z(54),
    //
    // 摘要:
    //     Windows 2000/XP: For any country/region, the ',' key
    OEM_COMMA(55),
    //
    // 摘要:
    //     Windows 2000/XP: For any country/region, the '.' key
    OEM_PERIOD(56),
    //
    // 摘要:
    //     SPACEBAR
    SPACE(62),
    //
    // 摘要:
    //     ENTER key
    RETURN(66),
    //
    // 摘要:
    //     BACKSPACE key
    BACK(67),
    //
    // 摘要:
    //     Used for miscellaneous characters; it can vary by keyboard. Windows 2000/XP:
    //     For the US standard keyboard, the '`~' key
    OEM_3(68),
    //
    // 摘要:
    //     Windows 2000/XP: For any country/region, the '-' key
    OEM_MINUS(69),
    //
    // 摘要:
    //     Used for miscellaneous characters; it can vary by keyboard. Windows 2000/XP:
    //     For the US standard keyboard, the '[{' key
    OEM_4(71),
    //
    // 摘要:
    //     Used for miscellaneous characters; it can vary by keyboard. Windows 2000/XP:
    //     For the US standard keyboard, the ']}' key
    OEM_6(72),
    //
    // 摘要:
    //     Used for miscellaneous characters; it can vary by keyboard. Windows 2000/XP:
    //     For the US standard keyboard, the '\|' key
    OEM_5(73),
    //
    // 摘要:
    //     Used for miscellaneous characters; it can vary by keyboard. Windows 2000/XP:
    //     For the US standard keyboard, the ';:' key
    OEM_1(74),
    //
    // 摘要:
    //     Used for miscellaneous characters; it can vary by keyboard. Windows 2000/XP:
    //     For the US standard keyboard, the 'single-quote/double-quote' key
    OEM_7(75),
    //
    // 摘要:
    //     Used for miscellaneous characters; it can vary by keyboard. Windows 2000/XP:
    //     For the US standard keyboard, the '/?' key
    OEM_2(76),
    //
    // 摘要:
    //     Windows 2000/XP: For any country/region, the '+' key
    OEM_PLUS(70),
    //
    // 摘要:
    //     SHIFT key
    SHIFT(1000);

    companion object {
        infix fun fromInt(value: Int): VirtualKeyCode? = VirtualKeyCode.values().firstOrNull { it.i == value }
    }
}

enum class KeyState {
    Down,
    Up,
    Click,
}

class KeyboardCmd: Cmd {
    // Correct message format:
    //---------------------------------------------------------------------------------
    // data = keyCode + "," + state
    //---------------------------------------------------------------------------------

    lateinit var virtualKeyCode: VirtualKeyCode
        private set

    lateinit var keyState: KeyState
        private set

    // For sender.
    constructor(virtualKeyCode: VirtualKeyCode, keyState: KeyState) {
        this.virtualKeyCode = virtualKeyCode
        this.keyState = keyState
        cmdType = CmdType.Keyboard
    }

    // For receiver.
    constructor(buffer: ByteArray) {
        data = buffer
        cmdType = CmdType.Keyboard
    }

    override fun Encode(): ByteArray {
        data = String.format("${virtualKeyCode.name},${keyState.name}").toByteArray(Charsets.US_ASCII)
        return AddHeadTail(data)
    }

    override fun Decode() {
        val tmp = data.toString(Charsets.US_ASCII).split(",")
        try {
            virtualKeyCode = VirtualKeyCode.valueOf(tmp[0])
        } catch (ex: IllegalArgumentException) {

        }
        try {
            keyState = KeyState.valueOf(tmp[1])
        } catch (ex: IllegalArgumentException) {

        }
    }
}