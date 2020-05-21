import React from "react";
import L from 'leaflet';
import "./App.css";
import AutosizeInput from 'react-input-autosize'
var overpass = require("query-overpass")

require('leaflet-routing-machine'); // Adds L.Routing onto L
require('lrm-graphhopper'); // Adds L.Routing.GraphHopper onto L.Routing

class App extends React.Component {
  constructor(props){
    super(props);
    this.state = {
      activeMapFeature: null,
      searchedPlace: "",
      mapData: null,
      query: "[out:json];node(57.7,11.9,57.8,12.0)[amenity=bar];out;",
      mapCenter: [0.0, 0.0],
      map: null,
      geoJsonLayer: null,
      
      time: '',
      length: ''
    };
    this.handleChange = this.handleChange.bind(this);
    this.searchPlace = this.searchPlace.bind(this);
    this.callback = this.callback.bind(this);
  }

  handleChange(event){
    const target = event.target;
    const value = target.value;
    const name = target.name;
    this.setState({
      [name]: value
    });
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
        //result: JSON.stringify(data),
        geoJsonLayer: geoJsonLayer
      });
    }
  }

  searchPlace(event){
    overpass(this.state.query, this.callback);
  }

  componentDidMount(){
    var map = L.map('map').setView([20., -105], 3);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
			maxZoom: 18,
			attribution: '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors',
			id: 'mapbox/light-v9'
    }).addTo(map);

    var wayPoint1 = L.latLng(57.74, 11.94);
    var wayPoint2 = L.latLng(57.6792, 11.949);

    let rWP1 = new L.Routing.Waypoint();
    rWP1.latLng = wayPoint1;    

    let rWP2 = new L.Routing.Waypoint();
    rWP2.latLng = wayPoint2;


    function createButton(label, container) {
      var btn = L.DomUtil.create('button', '', container);
      btn.setAttribute('type', 'button');
      btn.innerHTML = label;
      return btn;
    }
  
    map.on('click', function(e) {
        var container = L.DomUtil.create('div'),
            startBtn = createButton('Start from this location', container),
            destBtn = createButton('Go to this location', container);
    
        L.popup()
            .setContent(container)
            .setLatLng(e.latlng)
            .openOn(map);
    });
    // http://www.liedman.net/leaflet-routing-machine/tutorials/interaction/

    var control = L.Routing.control({
        lineOptions:{
          styles: [{color: 'red', opacity: 1, weight: 5}],
          addWaypoints: false
        },
        routeWhileDragging: true,
        plan: L.Routing.plan([wayPoint1,wayPoint2], {
          createMarker: function(i, wp) {
            return L.marker(wp.latLng, {
              draggable: true
            });
          }
        }),
        // showAlternatives: true,
        // addWaypoints: false, 
        // draggableWaypoints: false, 
        // routeWhileDragging: false, 
        // show: false,
        // collapsible: true,
        router: L.Routing.graphHopper('9f251f13-8860-4ec1-b248-29334abc9e46'),
    }).addTo(map);

    let route = control.getRouter();
    console.log(`control = ${JSON.stringify(route)}`)
    console.log(`control = ${JSON.stringify(control.getPlan().getWaypoints())}`)
    console.log(`time = ${control.route.time}`)

    L.Routing.errorControl(control).addTo(map);

    var geoJsonLayer = L.geoJSON();
    geoJsonLayer.addTo(map)

    this.setState({
      map: map,
      geoJsonLayer: geoJsonLayer
    })
  }

  render(){
    return (
      <div>
        <div>
          <input type="textbox" onChange={this.handleChange} name="searchedPlace"></input>
        </div>
        <div>
          <label>Searched Place: </label>
          <label>{this.state.searchedPlace}</label>
          <br />
          <label>Distance: '{this.state.length}'</label>
          <br />
          <label>Time: '{this.state.time}'</label>
        </div>
        <br />
        <div>
          <AutosizeInput onChange={this.handleChange} name="query" value={this.state.query}/>
          <button onClick = {this.searchPlace}>Search</button>
        </div>
        <div>
          <label>Active Feature: </label>
          <br />
          <label>{JSON.stringify(this.state.activeMapFeature)}</label>
        </div>
        <div className = "container">
          <div id="map"></div>
        </div>
      </div>
    );
  }
}

export default App;
