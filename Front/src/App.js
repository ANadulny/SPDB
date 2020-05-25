import React from "react";
import L, { marker } from 'leaflet';
import "./App.css";
import AutosizeInput from 'react-input-autosize'
var overpass = require("query-overpass")

require('leaflet-routing-machine'); // Adds L.Routing onto L
require('lrm-graphhopper'); // Adds L.Routing.GraphHopper onto L.Routing

class App extends React.Component {
  constructor(props){
    super(props);
    this.state = {
      startingPoint: { lat: 0.0, lng: 0.0 },
      activeMapFeature: null,
      mapData: null,
      query: "",
      startingPointMarker: new L.Marker([0,0]),
      geoJsonLayer: null,
      
      time: '',
      length: '',
      routeLength: null,
      searchedFeatures: [new TagList()],
      isAnd: false,
      radius: 0
    };
    this.handleChange = this.handleChange.bind(this);
    this.searchPlace = this.searchPlace.bind(this);
    this.callback = this.callback.bind(this);
    this.handleStartingPointChange = this.handleStartingPointChange.bind(this);
    this.handleFeatureTagSelect = this.handleFeatureTagSelect.bind(this);
  }

  onEachFeature(feature, layer) {
    // does this feature have a property named popupContent?
    if (feature.properties && feature.properties.popupContent) {
        layer.bindPopup(feature.properties.popupContent);
    }else if(feature.properties && feature.properties.tags.name){
      	layer.bindPopup(feature.properties.tags.name);
    }
  }

  callback(error, data){
    if(error){
      alert("error: " + error);
    }
    else{
      var geoJsonLayer = L.geoJSON(data, {onEachFeature: this.onEachFeature});
      
      this.state.geoJsonLayer.clearLayers();
      geoJsonLayer.addTo(this.state.map);
      this.setState({
        mapData: data,
        result: JSON.stringify(data),
        geoJsonLayer: geoJsonLayer
      });
    }
  }

  searchPlace(event){
    //Do stuff
    alert("searching stuff!");
    //overpass(this.state.query, this.callback);
  }

  componentDidMount(){
    var map = L.map('map').setView([52.2366, 21.0030], 12);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
			maxZoom: 18,
			attribution: '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors',
			id: 'mapbox/light-v9'
    }).addTo(map);

    function createButton(label, container) {
      var btn = L.DomUtil.create('button', '', container);
      btn.setAttribute('type', 'button');
      btn.innerHTML = label;
      return btn;
    }

    let markerGroup = L.layerGroup().addTo(map);
    this.state.startingPointMarker.addTo(markerGroup);

    map.on('click', function(e) {
        var container = L.DomUtil.create('div'),
            startBtn = createButton('Start from this location', container);
        startBtn.onclick = function () { 
           console.log("Setting new starting point");
           console.log('lat: ' + e.latlng.lat + ' lng:' + e.latlng.lng);
           this.state.startingPointMarker.setLatLng([e.latlng.lat, e.latlng.lng]);
           this.setState({
            startingPoint: { lat: e.latlng.lat, lng: e.latlng.lng }
           });
           map.closePopup();
         }.bind(this);
        L.popup()
            .setContent(container)
            .setLatLng(e.latlng)
            .openOn(map);
    }.bind(this));

    var control = L.Routing.control({
        router: L.Routing.graphHopper('9f251f13-8860-4ec1-b248-29334abc9e46'),
    });
    control.addTo(map);

    L.Routing.errorControl(control).addTo(map);

    var geoJsonLayer = L.geoJSON();
    geoJsonLayer.addTo(map)

    this.setState({
      map: map,
      geoJsonLayer: geoJsonLayer
    })
  }

  async validateForm(event) {
    console.log("validateForm");
    // var x, text;
    // x = document.forms["userSearchingInputData"]["lat"].value;
    // if (isNaN(x) || x < 1 || x > 10) {
    //   text = "Input not valid";
    // } else {
    //   text = "Input OK";
    // }
    // document.getElementById("demo").innerHTML = text;
    // document.getElementById("submit").disabled = false;
  }

  handleChange(event){
    const target = event.target;
    const value = target.value;
    const name = target.name;
    this.setState({
      [name]: value
    });
  }

  handleStartingPointChange(event){
    const target = event.target;
    const value = target.value;
    const name = target.name;
    console.log(name + ' ' + value);
    if(this.state.startingPointMarker != null){
    if(name === 'lat'){
      this.setState({
        startingPoint: { lat: value, lng: this.state.startingPoint.lng }
      }, () => {
        if(this.state.startingPointMarker != null){
          this.state.startingPointMarker.setLatLng(this.state.startingPoint);
          console.log(this.state.startingPointMarker.getLatLng());
      }});
    }else{
      this.setState({
        startingPoint: { lat: this.state.startingPoint.lat, lng: value }
      }, () => {
        if(this.state.startingPointMarker != null){
          this.state.startingPointMarker.setLatLng(this.state.startingPoint);
          console.log(this.state.startingPointMarker.getLatLng());
      }});
    }
    }
  }

  handleFeatureTagSelect(event){
    const target = event.target;
    const value = target.value.split(",");
    const name = target.name;
    if(event.target.value === "ignore")
      return;
    
    var featuresList = this.state.searchedFeatures[Number(value[0])];
    console.log(this.state.searchedFeatures);
    console.log(featuresList);
    featuresList.elemList[value[0]] = value[2];
    this.setState(state => {
      const list = state.searchedFeatures.map((item, j) => {
        if (Number(value[1]) === j) {
          return featuresList
        } else {
          return item;
        }
      });
      return {
        list,
      };
    }, () => this.render());
    alert("you've selected: " + event.target.value);
  }

  render(){
    var availableFeatures = new AvailableFeatures();
    var searchedFeatures = <tr></tr>
    var i = -1;
    searchedFeatures = <div><table>
      <tr>
      <td><select onChange = {this.handleFeatureTagSelect}>
        <option value="ignore">---Please select type---</option>
        {Object.entries(availableFeatures.features).map(([key, value]) => {
          i++;
          return (<option value={[0, i, key]} class="boldOption">{key}</option>)
      })}
      </select></td>
      <td><label>Distance: </label><input></input></td>
      </tr>
    </table>
    <label>Koniunkcja? </label>
    <input type="checkbox"></input>
    </div>;

    return (
      <div>
        <div name = 'startingPointRow'>
          <label>Starting point: </label>
          <label>latitude: </label>
          <input type='textbox' name='lat' value={this.state.startingPoint.lat} onChange={this.handleStartingPointChange}></input>
          <label>longitude:</label>
          <input type='textbox' name='lng' value={this.state.startingPoint.lng} onChange={this.handleStartingPointChange}></input>
          <label>radius:</label>
          <input type='textbox' name='radius' value={this.state.radius} onChange={this.handleChange}></input>
        </div>
        <div class = "searchedFeatures">
          <table align="center">
          {searchedFeatures}
          </table>
        </div>
        <div>
          <button onClick={this.searchPlace}>Submit</button>
        </div>
      <br />
      <div>
        <AutosizeInput onChange={this.handleChange} name="query" value={this.state.query}/>
        <button onClick = {this.searchPlace}>Search</button>
      </div>
      <div className = "container">
        <div id="map"></div>
      </div>
    </div>
    );
  }
}

class AvailableFeatures{
  features = {
    "amenity": ["bicycle_parking", "bicycle_rental", "bus_station", "car_wash"],
    "natural": [{
      "water": ["lake, rivier, pond"]
    }, "beach", "wood"],
    "tourism": ["hotel", "guest_house", "camp_site"]
  }
}
class TagList{
  elemList = [null, null, null];
  //0 - cat
  //1 - subcat/tag
  //2 - tag for subcat
}

export default App;
