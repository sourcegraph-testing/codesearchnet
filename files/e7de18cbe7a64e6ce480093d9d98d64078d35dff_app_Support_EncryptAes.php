<?php namespace Alfredoem\Ragnarok\Support;

class EncryptAes
{
    private static $key = "2nfvH3rVmziXRGTziuWu0GN3mcP69ahA";
    protected static $BASE64CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    private static $iv = 'ecWEQGPaK=dIhOBL/VenYkEYTNsqZGWQ';

    public static function encrypt($data)
    {
        $b = mcrypt_get_block_size(MCRYPT_RIJNDAEL_256, MCRYPT_MODE_CBC);
        $enc = mcrypt_module_open(MCRYPT_RIJNDAEL_256, '', MCRYPT_MODE_CBC, '');

        ////////////////////////
        //cambiamos el key con un caracter random de BASE64CHARS
        ////////////////////////

        //copia el key para modificar
        $skey = self::$key;

        //coje un numero aleatorio
        $posRand = rand(0, strlen(self::$BASE64CHARS)-1);
        //extrae el caracte en la posicion random
        $charRand = self::$BASE64CHARS{$posRand};

        //reemplazamos el primer caracter del key con el charRand
        $skey{ord($charRand)&0x1f} = $charRand;

        mcrypt_generic_init($enc, $skey, self::$iv);
        // PKCS7 Padding from: https://gist.github.com/1077723
        $dataPad = $b-(strlen($data)%$b);
        $data .= str_repeat(chr($dataPad), $dataPad);

        $encrypted_data = mcrypt_generic($enc, $data);

        mcrypt_generic_deinit($enc);
        mcrypt_module_close($enc);

        return base64_encode($encrypted_data).$charRand;

    }

    public static function dencrypt($encryptedData)
    {
        $enc = mcrypt_module_open(MCRYPT_RIJNDAEL_256, '', MCRYPT_MODE_CBC, '');

        //copia key original
        $skey = self::$key;

        //Sacamos el $randKey
        $charRand = substr($encryptedData, -1);

        //modifica encryotedData quita ultimo elemento
        $encryptedData = substr($encryptedData, 0, strlen($encryptedData)-1);
        $encryptedData = base64_decode($encryptedData);

        //reemplaza el primer el elemento
        $skey{ord($charRand)&0x1f} = $charRand;

        mcrypt_generic_init($enc, $skey, self::$iv);

        $data = mdecrypt_generic($enc, $encryptedData);
        mcrypt_generic_deinit($enc);
        mcrypt_module_close($enc);


        // PKCS7 Padding from: https://gist.github.com/1077723
        $dataPad = ord($data[strlen($data)-1]);

        return substr($data, 0, -$dataPad);
    }
}
