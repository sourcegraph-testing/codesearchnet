<?php

namespace Weew\Url;

class UrlBuilder implements IUrlBuilder {
    /**
     * @var string
     */
    protected $pattern = ':scheme:credentials:host:port:path:query:fragment';

    /**
     * @param IUrl $url
     * @param bool $encode
     *
     * @return string
     */
    public function build(IUrl $url, $encode = false) {
        $pattern = $this->pattern;
        $pattern = $this->setProtocol($pattern, $url, $encode);
        $pattern = $this->setCredentials($pattern, $url, $encode);
        $pattern = $this->setHost($pattern, $url, $encode);
        $pattern = $this->setPort($pattern, $url, $encode);
        $pattern = $this->setPath($pattern, $url, $encode);
        $pattern = $this->setQuery($pattern, $url, $encode);
        $pattern = $this->setFragment($pattern, $url, $encode);

        return $pattern;
    }

    /**
     * @param string $tld
     * @param string $domain
     * @param string $subdomain
     *
     * @return string
     */
    public function buildHost($tld, $domain, $subdomain) {
        $parts = [];

        if ($tld !== null) {
            array_unshift($parts, $tld);
        }

        if ($domain !== null) {
            array_unshift($parts, $domain);
        }

        if ($subdomain !== null) {
            array_unshift($parts, $subdomain);
        }

        $host = implode('.', $parts);

        return $host;
    }

    /**
     * @param string $pattern
     * @param IUrl $url
     * @param bool $encode
     * @param string $key
     *
     * @return string
     */
    protected function setProtocol($pattern, IUrl $url, $encode = false, $key = ':scheme') {
        $scheme = $url->getProtocol();

        if ($scheme) {
            $scheme = s('%s://', $scheme);
        }

        $pattern = s($pattern, [$key => $scheme]);

        return $pattern;
    }

    /**
     * @param string $pattern
     * @param IUrl $url
     * @param bool $encode
     * @param string $key
     *
     * @return string
     */
    protected function setCredentials($pattern, IUrl $url, $encode = false, $key = ':credentials') {
        $username = $url->getUsername();
        $password = $url->getPassword();
        $credentials = '';

        if ($encode) {
            $username = rawurlencode($username);
            $password = rawurlencode($password);
        }

        if ($username && $password) {
            $credentials = s('%s:%s@', $username, $password);
        }

        $pattern = s($pattern, [$key => $credentials]);

        return $pattern;
    }

    /**
     * @param string $pattern
     * @param IUrl $url
     * @param bool $encode
     * @param string $key
     *
     * @return string
     */
    protected function setHost($pattern, IUrl $url, $encode = false, $key = ':host') {
        $host = $url->getHost();
        $pattern = s($pattern, [$key => $host]);

        return $pattern;
    }

    /**
     * @param string $pattern
     * @param IUrl $url
     * @param bool $encode
     * @param string $key
     *
     * @return string
     */
    protected function setPort($pattern, IUrl $url, $encode = false, $key = ':port') {
        $port = $url->getPort();

        if ($port) {
            $port = s(':%s', $port);
        }

        $pattern = s($pattern, [$key => $port]);

        return $pattern;
    }

    /**
     * @param string $pattern
     * @param IUrl $url
     * @param bool $encode
     * @param string $key
     *
     * @return string
     */
    protected function setPath($pattern, IUrl $url, $encode = false, $key = ':path') {
        $path = $url->getPath();
        $pattern = s($pattern, [$key => $path]);

        return $pattern;
    }

    /**
     * @param string $pattern
     * @param IUrl $url
     * @param bool $encode
     * @param string $key
     *
     * @return string
     */
    protected function setQuery($pattern, IUrl $url, $encode = false, $key = ':query') {
        $query = $url->getQuery()->toString($encode);

        if ($query) {
            $query = s('?%s', $query);
        }

        $pattern = s($pattern, [$key => $query]);

        return $pattern;
    }

    /**
     * @param string $pattern
     * @param IUrl $url
     * @param bool $encode
     * @param string $key
     *
     * @return string
     */
    protected function setFragment($pattern, IUrl $url, $encode = false, $key = ':fragment') {
        $fragment = $url->getFragment();

        if ($fragment) {
            $fragment = s('#%s', $fragment);
        }

        $pattern = s($pattern, [$key => $fragment]);

        return $pattern;
    }
}
