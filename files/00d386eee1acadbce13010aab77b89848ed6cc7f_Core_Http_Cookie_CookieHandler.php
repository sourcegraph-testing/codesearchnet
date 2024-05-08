<?php
namespace Core\Http\Cookie;

/**
 * CookieHandler.php
 *
 * @author Michael "Tekkla" Zorn <tekkla@tekkla.de>
 * @copyright 2016
 * @license MIT
 */
class CookieHandler
{

    private $cookies = [];

    /**
     * Send all cookies to the browser
     *
     * @return boolean
     */
    public function send()
    {
        /* @var $cookie \Core\Http\Cookie\CookieInterface */
        foreach ($this->cookies as $cookie) {
            setcookie($cookie->getName(), $cookie->getValue(), $cookie->getExpire(), $cookie->getPath(), $cookie->getDomain(), $cookie->getSecure(), $cookie->getHttponly());
        }
    }

    /**
     * Creates a cookie object, adds it to the cookies stack and returns an reference to this cookie
     *
     * @param string $name
     *            Name of cookie to create
     *
     * @return \Core\Http\Cookie\CookieInterface
     */
    public function &createCookie(string $name)
    {
        $cookie = new Cookie();
        $cookie->setName($name);

        $this->addCookie($cookie);

        return $cookie;
    }

    /**
     * Adds a cookie to the cookies stack
     *
     * The cookie will be stored by using it's name as index for the cookies stack, so it's possible that an already
     * present cookie with the same name gets replaced by the one you are adding.
     *
     * @param CookieInterface $cookie
     *            The cookie to add
     * @param bool $overwrite
     *            Optional flag to control overwriting of an cookie stored with the same name.
     *            Instead of replacing an existing cookie a CookieException will be thrown.
     *
     * @throws CookieException
     */
    public function addCookie(CookieInterface $cookie, bool $overwrite = true)
    {
        $cookie_name = $cookie->getName();

        if ($overwrite == false && array_key_exists($cookie_name, $this->cookies)) {
            Throw new CookieException(sprintf('A cookie with name "%s" already exists.', $cookie_name));
        }

        $this->cookies[$cookie_name] = $cookie;
    }

    /**
     * Removes a cookie by it's name
     *
     * @param string $name
     *            Cookiename
     */
    public function remove($name)
    {
        $cookie = $this->createCookie($name);
        $cookie->setExpire(1);
    }
}
