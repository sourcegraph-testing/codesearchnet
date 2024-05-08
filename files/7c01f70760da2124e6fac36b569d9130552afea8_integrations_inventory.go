package integrations

import (
  "net/http"
  "encoding/json"
  "fmt"
  "errors"
  "log"
  "time"
  "io/ioutil"
  "labix.org/v2/mgo/bson"
  "os"
  "strings"
  "crypto/tls"
  cfenv "github.com/cloudfoundry-community/go-cfenv"
)

// New - create an inventory service wrapper.
func (client *MyInventoryClient) New(appEnv *cfenv.App) *MyInventoryClient {

  if strings.ToUpper(os.Getenv("DISPLAY_NEW_SERVICES")) == "YES" {
    client.Enabled = true
    client.ServiceBaseURL = getServiceBinding("inventory-service", "target-url", appEnv)
  } else {
    client.Enabled = false
  }
  return client
}

// NewWithURL - create client directly with URL. Used for testing.
func (client *MyInventoryClient) NewWithURL(url string) *MyInventoryClient {
  client.Enabled = true
  client.ServiceBaseURL = url
  return client
}

// GetInventoryItems - query the inventory items from the inventory service
func (client *MyInventoryClient) GetInventoryItems() (result []InventoryItem, err error) {
  if !client.Enabled {
    return
  }
  effectiveURL := fmt.Sprint(client.ServiceBaseURL, "/inventory")
  responseMessage, err := getRawResponseFromURL(effectiveURL)

  if err != nil {
    log.Println(err)
    return
  }

  var inventoryItems []inventoryServiceItem
  err = json.Unmarshal(responseMessage.Data, &inventoryItems)
  result = make([]InventoryItem, len(inventoryItems))
  if strings.ToUpper(responseMessage.Status) != "SUCCESS" {
    err = errors.New(responseMessage.Message)
    return
  }
  for idx, element := range inventoryItems {
    result[idx] = InventoryItem{SKU: element.SKU, Size: element.Size, Tier: element.Tier, OfferingType: element.OfferingType, Status: element.Status, ID: element.ID.Hex()}
    if len(element.LeaseID) > 0 {
      theLease, theError := client.GetLease(element.LeaseID)
      if theError != nil {
        log.Println(theError)
      }
      result[idx].CurrentLease = theLease
    }
  }
  return
}

// GetLease - obtains an individual lease, typically by referencing it from a parent
// inventory item
func (client *MyInventoryClient) GetLease(leaseID string) (lease InventoryLease, err error) {
  if !client.Enabled {
    return
  }
  effectiveURL := fmt.Sprint(client.ServiceBaseURL, "/leases/", leaseID)
  log.Printf("Fetching lease at %s", effectiveURL)
  response, err := getRawResponseFromURL(effectiveURL)

  if err != nil {
    log.Println(err)
    return
  }

  var remoteLease inventoryLease
  err = json.Unmarshal(response.Data, &remoteLease)
  lease = InventoryLease{Username: remoteLease.User, DaysUntilExpires: getDaysRemainingFromEndDate(remoteLease.EndDate)}

  return
}

// LeaseInventoryItem acquires a new lease on an inventory item from the inventory service.
func (client *MyInventoryClient) LeaseInventoryItem(inventoryItemID string, user string, duration int) (lease *InventoryLease, err error) {
  if !client.Enabled {
    return
  }

  effectiveURL := fmt.Sprint(client.ServiceBaseURL, "/leases")
  log.Printf("Attempting to acquire new lease from URL %s", effectiveURL)
  response, err := getRawResponseFromURL(effectiveURL)
  if err != nil {
    log.Println(err)
    return
  }

  if strings.ToUpper(response.Status) != "SUCCESS" {
    lease = nil
    err = errors.New(response.Message)
    return
  }

  var remoteLease inventoryLease
  err = json.Unmarshal(response.Data, &remoteLease)
  lease = &InventoryLease{Username: remoteLease.User, DaysUntilExpires: duration}

  return
}

func (client *MyInventoryClient) String() string {
  if client.Enabled {
    return fmt.Sprintf("{InventoryClient pointing at %s }", client.ServiceBaseURL)
  }
  return fmt.Sprintf("{InventoryClient DISABLED}")
}

func getRawResponseFromURL(URL string) (response inventoryResponseMessage, err error) {

  // Currently allowing invalid certs to talk to inventory service.
  // required to talk to inventory service locally or in dev.
  // TODO make this a configurable toggle so we can enforce it in prod.
  tr := &http.Transport{
        TLSClientConfig: &tls.Config{InsecureSkipVerify: true},
    }
  httpclient := &http.Client{Transport: tr}
  r, err := httpclient.Get(URL)
  if err != nil {
    log.Println(err)
    return
  }
  rawresponse, err := ioutil.ReadAll(r.Body)
  err = json.Unmarshal(rawresponse, &response)
  r.Body.Close()
  return
}

func getDaysRemainingFromEndDate(endDate string) int {
  // 2015-09-15 04:42:39.390008575 +0000 UTC
  //t, err := time.Parse("2006-01-02", endDate)
  t, err := time.Parse("2006-01-02 15:04:05.000000000 +0000 UTC", endDate)
  if err != nil {
      log.Println(err)
      return 0
  }
  now := time.Now()
  return diffDays(t, now)
}

func diffDays(t1 time.Time, t2 time.Time) int {
    return int(t1.Sub(t2) / (24 * time.Hour))
}


func getServiceBinding(serviceName string, serviceURIName string, appEnv *cfenv.App) (serviceURI string) {
	if service, err := appEnv.Services.WithName(serviceName); err == nil {
		if serviceURI = service.Credentials[serviceURIName].(string); serviceURI == "" {
			panic(fmt.Sprintf("we pulled an empty connection string %s from %v - %v", serviceURI, service, service.Credentials))
		}

	} else {
		panic(fmt.Sprint("Experienced an error trying to grab service binding information:", err.Error()))
	}
	return
}

// Private types
// These are the types that we use to unmarshal json from the inventory service
// They will be converted into our (portal) types for consumption by the angular
// controller.
// ------------------------------------------------

type inventoryResponseMessage struct {
    //Status returns a string indicating [success|error|fail]
    Status string `json:"status"`
    //Data holds the payload of the response
    Data    json.RawMessage
    //Message contains the nature of an error
    Message string `json:"message,omitempty"`
    //Meta contains information about the data and the current request
    Meta map[string]interface{} `json:"_metaData,omitempty"`
    //Links contains [prev|next] links for paginated responses
    Links map[string]interface{} `json:"_links,omitempty"`
}

type inventoryLease struct {
  ID              bson.ObjectId          `bson:"_id,omitempty"`
	InventoryItemID bson.ObjectId          `bson:"inventory_item_id,omitempty"`
	User            string                 `json:"user"`
	Duration        int                    `json:"duration"`
	StartDate       string                 `json:"start_date"`
	EndDate         string                 `json:"end_date"`
	Status          string                 `json:"status"`
	//Attributes      json.RawMessage        `json:"attributes"`
}

type leaseRequest struct {
  InventoryItemID   bson.ObjectId       `json:"inventory_item_id"`
  User              string              `json:"user"`
  Duration          int                 `json:"duration"`
}

type inventoryServiceItem struct {
 ID           bson.ObjectId          `bson:"_id,omitempty"`
 SKU          string                 `json:"sku"`
 Tier         int                    `json:"tier"`
 OfferingType string                 `json:"offering_type"`
 Size         string                 `json:"size"`
 Attributes   map[string]interface{} `json:"attributes"`
 Status       string                 `json:"status"`
 LeaseID      string                 `json:"lease_id"`
}
