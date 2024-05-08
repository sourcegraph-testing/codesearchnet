import unittest
from HTML.Auto import Encoder

class TestTagAttrs(unittest.TestCase):

    def test_lower(self):

        encoder = Encoder()

        self.assertEqual(
            '&amp;&lt;&gt;&quot;&apos;',
            encoder.encode( '&<>"\'' ),
            'default chars encoded when chars is nil'
        )

        self.assertEqual(
            '&amp;&lt;&gt;&quot;&apos;',
            encoder.encode( '&<>"\'', '' ),
            'encodes when chars is empty'
        )

        self.assertEqual(
            'h&#101;llo',
            encoder.encode( 'hello', 'e' ),
            'requested chars encoded correctly'
        )

        self.assertEqual(
            'hell&#48;',
            encoder.encode( 'hell0', 0 ),
            'zero encodes correctly'
        )

        self.assertEqual(
            '&amp;b&#97;r',
            encoder.encode( '&bar', 'a&' ),
            'ampersand is not double encoded'
        )

        self.assertEqual(
            'hello',
            encoder.encode( 'hello' ),
            'no encodes when default chars is nil'
        )

        self.assertEqual(
            'hello',
            encoder.encode( 'hello', '' ),
            'no encodes when default chars is empty'
        )

        deadbeef = chr(222) + chr(173) + chr(190) + chr(239)

        self.assertEqual(
            '&THORN;&shy;&frac34;&iuml;',
            encoder.encode( deadbeef, deadbeef ),
            'hex codes encoded correctly'
        )

        self.assertEqual(
            '&THORN;&shy;&frac34;&iuml;',
            encoder.encode( deadbeef ),
            'hex codes encoded correctly when chars is nil'
        )

        self.assertEqual(
            '&THORN;&shy;&frac34;&iuml;',
            encoder.encode( deadbeef, '' ),
            'hex codes encoded correctly when chars is empty'
        )

    def test_numeric(self):

        encoder = Encoder()

        self.assertEqual(
            '&#x26;&#x3C;&#x3E;&#x22;&#x27;',
            encoder.encode_hex( '&<>"\'' ),
            'default chars encoded numerically correctly'
        )

        self.assertEqual(
            '&amp;&lt;&gt;&quot;&apos;',
            encoder.encode( '&<>"\'' ),
            'does not impact default chars encoded correctly'
        )

        self.assertEqual(
            'hello',
            encoder.encode_hex( 'hello', '' ),
            'no chars encoded when no chars requested'
        )

        self.assertEqual(
            'h&#x65;llo',
            encoder.encode_hex( 'hello', 'e' ),
            'requested chars encoded correctly'
        )

        self.assertEqual(
            'h&#101;llo',
            encoder.encode( 'hello', 'e' ),
            'requested chars encoded correctly'
        )

    def test_upper(self):
        return

        #return unless RUBY_VERSION > '1.8.7'
        encoder = Encoder()

        str = eval( '8224.chr(Encoding::UTF_8)' )
        self.assertEqual(
            '&dagger;',
            encoder.encode( str, str ),
            'unicode char encoded correctly'
        )

        str = eval( '9824.chr(Encoding::UTF_8)+9827.chr(Encoding::UTF_8)+9829.chr(Encoding::UTF_8)+9830.chr(Encoding::UTF_8)' )
        self.assertEqual(
            '&spades;&clubs;&hearts;&diams;',
            encoder.encode( str, str ),
            'unicode chars encoded correctly'
        )


if __name__ == '__main__':
    unittest.main()
