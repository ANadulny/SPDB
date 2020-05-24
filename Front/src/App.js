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
      startingPoint: { lat: '', lng: '' },
      activeMapFeature: null,
      searchedPlace: "",
      mapData: null,
      query: "",
      map: null,
      geoJsonLayer: null,
      
      time: '',
      length: '',
      routeLength: null
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
        result: JSON.stringify(data),
        geoJsonLayer: geoJsonLayer
      });
    }
  }

  searchPlace(event){
    overpass(this.state.query, this.callback);
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

    map.on('click', function(e) {
        var container = L.DomUtil.create('div'),
            startBtn = createButton('Start from this location', container);
        startBtn.onclick = function () { 
           console.log("Setting new starting point");
           markerGroup.clearLayers()
           new L.Marker([e.latlng.lat, e.latlng.lng]).addTo(markerGroup);
         };
        L.popup()
            .setContent(container)
            .setLatLng(e.latlng)
            .openOn(map);
    });

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

  render(){
    return (
      <div>
        <form action="http://localhost:8080/data" name='userSearchingInputData' onSubmit={this.validateForm()} method="post">
          <div name = 'startingPointRow'>
            <label>Starting point: </label>
            <label>latitude: </label>
            <input type='textbox' name='lat'></input>
            <label>longitude:</label>
            <input type='textbox' name='lng'></input>
          </div>
          {/* <div> */}
            {/* <select type="selector"></input> */}
          {/* </div> */}
          <div>
           <p id="demo"></p>
            <input type="submit" value="Submit" />
          </div>
        </form>
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

export default App;
