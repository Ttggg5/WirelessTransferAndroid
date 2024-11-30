package com.example.wirelesstransferandroid.internetsocket.cmd

import com.example.wirelesstransferandroid.internetsocket.Indexes


class CmdDecoder {
    companion object {
        // Correct message format:
        //---------------------------------------------------------------------------------
        // <CmdType> + DataLength + Data + <!CmdType>
        //---------------------------------------------------------------------------------
        // CmdType: no limit.
        // DataLength: 7 bytes to present (max: 5242880 bytes = 5MB), fill 0 in front if it not full.
        // Data: max length is 5242880 bytes.
        private var frontSymbol: Byte = "<".toByteArray(Charsets.US_ASCII)[0]
        private var backSymbol: Byte = ">".toByteArray(Charsets.US_ASCII)[0]
        private var endSymbol: Byte = "!".toByteArray(Charsets.US_ASCII)[0]

        // Special decode (has cycle).
        fun DecodeCmd(buffer: ByteArray, indexes: Indexes): Cmd? {
            var cmd: Cmd? = null
            var cmdType: CmdType = CmdType.None
            var cmdStr = ""
            var length = indexes.endIndex - indexes.startIndex
            if (length < 0)
                length = buffer.size - indexes.startIndex + indexes.endIndex
            else if (length == 0) return null

            if (buffer[indexes.startIndex] == frontSymbol)
            {
                var tmpBuffer = ByteArray(length)
                var fl = length
                if (indexes.startIndex + length > buffer.size) {
                    fl = buffer.size - indexes.startIndex
                    buffer.copyInto(tmpBuffer, fl, 0, length - fl)
                }
                buffer.copyInto(tmpBuffer, 0, indexes.startIndex, indexes.startIndex + fl)

                // find cmd type
                var previousIndex = 1
                var curIndex = previousIndex
                try
                {
                    while (true)
                    {
                        if (tmpBuffer[curIndex] == backSymbol)
                        {
                            cmdStr = tmpBuffer.sliceArray(previousIndex..<curIndex).toString(Charsets.US_ASCII)
                            try {
                                cmdType = CmdType.valueOf(cmdStr)
                            } catch (ex: IllegalArgumentException) {
                                indexes.startIndex = indexes.endIndex
                                return null
                            }
                            break
                        }
                        curIndex++
                    }
                }
                catch (ex: IndexOutOfBoundsException)
                {
                    return null
                }

                // find data length
                previousIndex = curIndex + 1
                var dataLength = 0
                try {
                    dataLength = tmpBuffer.sliceArray(previousIndex..<previousIndex + 7).toString(Charsets.US_ASCII).toInt()
                } catch (ex: Exception) {
                    indexes.startIndex = indexes.endIndex
                    return null
                }

                // find end symbol
                previousIndex += 7
                curIndex = previousIndex + dataLength + 1

                if (curIndex > length) {
                    //indexes.startIndex = indexes.endIndex
                    return null
                }

                if (tmpBuffer[curIndex] == endSymbol)
                {
                    // create cmd class
                    var data = tmpBuffer.sliceArray(previousIndex..<previousIndex + dataLength)
                    cmd = CreateDecodeCmd(cmdType, data)

                    indexes.startIndex += curIndex + cmdStr.length + 2
                    if (indexes.startIndex >= buffer.size)
                        indexes.startIndex -= buffer.size

                    return cmd
                }
                else return null
            }
            else
            {
                indexes.startIndex = indexes.endIndex
                return null
            }
        }

        fun CreateDecodeCmd(cmdType: CmdType, data: ByteArray): Cmd? {
            var cmd: Cmd? = null
            when (cmdType) {
                CmdType.Alive -> {}
                CmdType.ClientInfo -> cmd = ClientInfoCmd(data)
                CmdType.FileData -> cmd = FileDataCmd(data)
                CmdType.FileInfo -> cmd = FileInfoCmd(data)
                CmdType.Reply -> cmd = ReplyCmd(data)
                CmdType.Screen -> cmd = ScreenCmd(data)
                CmdType.Keyboard -> cmd = KeyboardCmd(data)
                CmdType.Mouse -> cmd = MouseCmd(data)
                CmdType.MouseMove -> cmd = MouseMoveCmd(data)
                CmdType.Webcam -> {}
                CmdType.Request -> cmd = RequestCmd(data)
                CmdType.ScreenInfo -> cmd = ScreenInfoCmd(data)
                else -> return null
            }
            cmd?.Decode()
            return cmd
        }
    }
}