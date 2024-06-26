# -*- coding: utf-8 -*-
# Created by restran on 2018/3/7
from __future__ import unicode_literals, absolute_import

import binascii
from xml.sax.saxutils import escape as xml_escape_func
from xml.sax.saxutils import unescape as xml_unescape_func

from mountains.encoding import force_bytes, force_text


def to_uu(data):
    """
    uu编码
    :param data: 字符串
    :return: 编码后的字符串
    """
    r = binascii.b2a_uu(force_bytes(data))
    return force_text(r)


def from_uu(data):
    """
    解uu编码
    :param data: uu编码的字符串
    :return: 字符串
    """
    r = binascii.a2b_uu(data)
    return force_text(r)


def str2hex(s):
    """
    把一个字符串转成其ASCII码的16进制表示
    :param s: 要转换的字符串
    :return: ASCII码的16进制表示字符串
    """
    return force_text(binascii.b2a_hex(force_bytes(s)))


def hex2str(s):
    """
    把十六进制字符串转换成其ASCII表示字符串
    :param s: 十六进制字符串
    :return: 字符串
    """
    return force_text(binascii.a2b_hex(s))


base = [str(x) for x in range(10)] + [chr(x) for x in range(ord('A'), ord('A') + 6)]


def bin2dec(s):
    """
    bin2dec
    二进制 to 十进制: int(str,n=10)
    :param s:
    :return:
    """
    return int(s, 2)


def dec2bin(s):
    """
    dec2bin
    十进制 to 二进制: bin()
    :param s:
    :return:
    """
    if not isinstance(s, int):
        num = int(s)
    else:
        num = s
    mid = []
    while True:
        if num == 0:
            break
        num, rem = divmod(num, 2)
        mid.append(base[rem])

    return ''.join([str(x) for x in mid[::-1]])


def hex2dec(s):
    """
    hex2dec
    十六进制 to 十进制
    :param s:
    :return:
    """
    if not isinstance(s, str):
        s = str(s)
    return int(s.upper(), 16)


def dec2hex(s):
    """
    dec2hex
    十进制 to 八进制: oct()
    十进制 to 十六进制: hex()
    :param s:
    :return:
    """
    if not isinstance(s, int):
        num = int(s)
    else:
        num = s
    mid = []
    while True:
        if num == 0:
            break
        num, rem = divmod(num, 16)
        mid.append(base[rem])

    return ''.join([str(x) for x in mid[::-1]])


def hex2bin(s):
    """
    hex2tobin
    十六进制 to 二进制: bin(int(str,16))
    :param s:
    :return:
    """
    if len(s) % 2 != 0:
        s += '0'

    result = []
    for i in range(len(s) // 2):
        t = s[i * 2:(i + 1) * 2]
        x = dec2bin(hex2dec(t.upper()))
        padding_length = (8 - len(x) % 8) % 8
        # 每个16进制值（2个字符）进行转码，不足8个的，在前面补0
        x = '%s%s' % ('0' * padding_length, x)
        result.append(x)

    return ''.join(result)


def bin2hex(s):
    """
    bin2hex
    二进制 to 十六进制: hex(int(str,2))
    :param s:
    :return:
    """
    padding_length = (8 - len(s) % 8) % 8
    # 从前往后解码，不足8个的，在后面补0
    encode_str = '%s%s' % (s, '0' * padding_length)
    # 解码后是 0xab1234，需要去掉前面的 0x
    return hex(int(encode_str, 2))[2:].rstrip('L')


def str2dec(s):
    """
    string to decimal number.
    """
    if not len(s):
        return 0
    return int(str2hex(s), 16)


def dec2str(n):
    """
    decimal number to string.
    """
    s = hex(int(n))[2:].rstrip('L')
    if len(s) % 2 != 0:
        s = '0' + s
    return hex2str(s)


def str2bin(s):
    """
    String to binary.
    """
    ret = []
    for c in s:
        ret.append(bin(ord(c))[2:].zfill(8))
    return ''.join(ret)


def bin2str(b):
    """
    Binary to string.
    """
    ret = []
    for pos in range(0, len(b), 8):
        ret.append(chr(int(b[pos:pos + 8], 2)))
    return ''.join(ret)


def from_digital(s, num):
    """
    进制转换，从指定机制转到10进制
    :param s:
    :param num:
    :return:
    """
    if not 1 < num < 10:
        raise ValueError('digital num must between 1 and 10')
    return '%s' % int(s, num)


def to_digital(d, num):
    """
    进制转换，从10进制转到指定机制
    :param d:
    :param num:
    :return:
    """
    if not isinstance(num, int) or not 1 < num < 10:
        raise ValueError('digital num must between 1 and 10')

    d = int(d)
    result = []
    x = d % num
    d = d - x
    result.append(str(x))
    while d > 0:
        d = d // num
        x = d % num
        d = d - x
        result.append(str(x))
    return ''.join(result[::-1])


def xml_escape(data):
    return xml_escape_func(data)


def xml_un_escape(data):
    return xml_unescape_func(data)


def str2int(number_str, default_value=None):
    if number_str is None or number_str == '':
        return default_value
    try:
        return int(number_str)
    except Exception as e:
        return default_value


def s2n(s):
    """
    String to number.
    """
    if not len(s):
        return 0
    return int(s.encode("hex"), 16)


def n2s(n):
    """
    Number to string.
    """
    s = hex(n)[2:].rstrip("L")
    if len(s) % 2 != 0:
        s = "0" + s
    return s.decode("hex")


def s2b(s):
    """
    String to binary.
    """
    ret = []
    for c in s:
        ret.append(bin(ord(c))[2:].zfill(8))
    return "".join(ret)


def b2s(b):
    """
    Binary to string.
    """
    ret = []
    for pos in range(0, len(b), 8):
        ret.append(chr(int(b[pos:pos + 8], 2)))
    return "".join(ret)


import struct


def long_to_bytes(n, blocksize=0):
    """Convert an integer to a byte string.

    In Python 3.2+, use the native method instead::

        >>> n.to_bytes(blocksize, 'big')

    For instance::

        >>> n = 80
        >>> n.to_bytes(2, 'big')
        b'\x00P'

    If the optional :data:`blocksize` is provided and greater than zero,
    the byte string is padded with binary zeros (on the front) so that
    the total length of the output is a multiple of blocksize.

    If :data:`blocksize` is zero or not provided, the byte string will
    be of minimal length.
    """
    # after much testing, this algorithm was deemed to be the fastest
    s = b''
    n = int(n)
    pack = struct.pack
    while n > 0:
        s = pack('>I', n & 0xffffffff) + s
        n = n >> 32
    # strip off leading zeros
    for i in range(len(s)):
        if s[i] != b'\000'[0]:
            break
    else:
        # only happens when n == 0
        s = b'\000'
        i = 0
    s = s[i:]
    # add back some pad bytes.  this could be done more efficiently w.r.t. the
    # de-padding being done above, but sigh...
    if blocksize > 0 and len(s) % blocksize:
        s = (blocksize - len(s) % blocksize) * b'\000' + s
    return s


def bytes_to_long(s):
    """Convert a byte string to a long integer (big endian).

    In Python 3.2+, use the native method instead::

        >>> int.from_bytes(s, 'big')

    For instance::

        >>> int.from_bytes(b'\x00P', 'big')
        80

    This is (essentially) the inverse of :func:`long_to_bytes`.
    """
    acc = 0
    unpack = struct.unpack
    length = len(s)
    if length % 4:
        extra = (4 - length % 4)
        s = b'\000' * extra + s
        length = length + extra
    for i in range(0, length, 4):
        acc = (acc << 32) + unpack('>I', s[i:i + 4])[0]
    return acc
