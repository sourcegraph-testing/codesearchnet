require 'oauth2'

module MstdnIvory
  module Register
    def create_app(name, scopes = 'read', redirect_uri = 'urn:ietf:wg:oauth:2.0:oob', website = nil)
      request(:post, '/api/v1/apps', { client_name: name, redirect_uris: redirect_uri, scopes: scopes, website: website })
    end

    def create_authorization_url(client_id, client_secret, scope = 'read', redirect_uri = 'urn:ietf:wg:oauth:2.0:oob')
      oauth_client = OAuth2::Client.new(client_id, client_secret, site: self.base_url)
      oauth_client.auth_code.authorize_url(redirect_uri: redirect_uri, scope: scope)
    end

    def get_access_token(client_id, client_secret, authorization_code, redirect_uri = 'urn:ietf:wg:oauth:2.0:oob')
      oauth_client = OAuth2::Client.new(client_id, client_secret, site: self.base_url)
      self.token = oauth_client.auth_code.get_token(authorization_code, redirect_uri: redirect_uri).token
    end
  end
end
