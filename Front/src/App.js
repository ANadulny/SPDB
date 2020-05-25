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
      startingPointRadius: new L.circle([0,0],0),
      geoJsonLayer: null,
      
      time: '',
      length: '',
      routeLength: null,
      searchedObject: new TagList(),
      searchedFeatures: [new TagList()],
      isAnd: false,
      radius: 0
    };
    this.handleChange = this.handleChange.bind(this);
    this.searchPlace = this.searchPlace.bind(this);
    this.callback = this.callback.bind(this);
    this.handleStartingPointChange = this.handleStartingPointChange.bind(this);
    this.handleFeatureTagSelect = this.handleFeatureTagSelect.bind(this);
    this.removeSearchedFeature = this.removeSearchedFeature.bind(this);
    this.addNewRowForSearchedFeature = this.addNewRowForSearchedFeature.bind(this);
    this.handleDistanceForSearchedFeaturesChange = this.handleDistanceForSearchedFeaturesChange.bind(this);
    this.handleSearchedObjectTagSelect = this.handleSearchedObjectTagSelect.bind(this);
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
    console.log(this.state.searchedFeatures);
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
    this.state.startingPointRadius.addTo(map);

    map.on('click', function(e) {
        var container = L.DomUtil.create('div'),
            startBtn = createButton('Start from this location', container);
        startBtn.onclick = function () { 
           this.state.startingPointMarker.setLatLng([e.latlng.lat, e.latlng.lng]);
           this.state.startingPointRadius.setLatLng([e.latlng.lat, e.latlng.lng]);
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
    if(name === "radius"){
      this.state.searchedObject.distance = Number(value);
      this.state.startingPointRadius.setRadius(Number(value));
    }
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
          this.state.circle.setLatLng(this.state.startingPoint);
      }});
    }else{
      this.setState({
        startingPoint: { lat: this.state.startingPoint.lat, lng: value }
      }, () => {
        if(this.state.startingPointMarker != null){
          this.state.startingPointMarker.setLatLng(this.state.startingPoint);
      }});
    }
    }
  }

  handleFeatureTagSelect(event){
    const target = event.target;
    const value = target.value.split(",");
    const name = target.name;
    var featuresList = this.state.searchedFeatures[Number(value[0])];
    
    if(value[2] === "ignore"){
      for(var i = Number(value[1]); i < featuresList.elemList.length; i++){
        featuresList.elemList[i] = null;
      }
    }else{
      featuresList.elemList[value[1]] = value[2];
      for(var i = Number(value[1])+1; i<featuresList.elemList.length; i++){
        featuresList.elemList[i] = null;
      }
    }
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
    //alert("you've selected: " + event.target.value);
  }

  removeSearchedFeature(event){
    const target = event.target;
    const value = target.value;
    const name = target.name;
    console.log(value + name);
    alert("remove: " + value);
    //TO DO!
  }

  addNewRowForSearchedFeature(event){
    var featuresList = this.state.searchedFeatures;
    featuresList.push(new TagList());
    this.setState({
      searchedFeatures: featuresList
    }, () => this.render())
  }

  renderAdditionalFields(rowIndex, columnIndex, key, fun){
    console.log(key);
    if(key === null)
      return <td></td>;
    var toReturn; 
    var availableFeatures = new AvailableFeatures();
    if(Array.isArray(key)){
      for(var i = 0; i < key.length; i++){
        if(key[i] === null)
          return <td></td>;
      }
      var returnNow = true;
      for(var i = 0; i< availableFeatures.features[key[0]].length; i++){
        if(availableFeatures.features[key[0]][i][key[1]] !== undefined && typeof(availableFeatures.features[key[0]][i]) !== "string")
          returnNow = false;
      }
      if(returnNow)
        return <td></td>
      toReturn = <td>
        {availableFeatures.features[key[0]] !== undefined &&
          <select onChange = {fun}>
          <option value={[rowIndex, columnIndex, "ignore"]}>---Please select type---</option>
          {availableFeatures.features[key[0]].map((elem) => {
            if(elem[key[1]] !== undefined && typeof(elem) !== "string"){
              return elem[key[1]].map((elem1) =>{
                return <option value={[rowIndex, columnIndex, elem1]}>{elem1}</option>;
            });
            }
          })}
        </select>
      }</td>;
    }else{
      toReturn = <td>
        <select onChange = {fun}>
          <option value={[rowIndex, columnIndex, "ignore"]}>---Please select type---</option>
          {availableFeatures.features[key].map((elem) => {
            if(typeof(elem) === "string"){
              return <option value={[rowIndex, columnIndex, elem]}>{elem}</option>;
            }else{
              return Object.entries(elem).map(([key, value]) => {
                return <option value={[rowIndex, columnIndex, key]}>{key}</option>
              });
            }
          })}
        </select>
      </td>;
    }
    return toReturn;
  }

  handleDistanceForSearchedFeaturesChange(event){
    const target = event.target;
    const value = target.value;
    const name = target.name;

    var featuresList = this.state.searchedFeatures[Number(name)];
    featuresList.distance = Number(value);

    this.setState(state => {
      const list = state.searchedFeatures.map((item, j) => {
        if (Number(name) === j) {
          return featuresList
        } else {
          return item;
        }
      });
      return {
        list,
      };
    }, () => this.render());
  }

  handleSearchedObjectTagSelect(event){
    const target = event.target;
    const value = target.value.split(",");
    const name = target.name;

    var searchedObject = this.state.searchedObject;
    
    if(value[2] === "ignore"){
      for(var i = Number(value[1]); i < searchedObject.elemList.length; i++){
        searchedObject.elemList[i] = null;
      }
    }else{
      searchedObject.elemList[value[1]] = value[2];
      for(var i = Number(value[1])+1; i<searchedObject.elemList.length; i++){
        searchedObject.elemList[i] = null;
      }
    }
    console.log('tag: ' + searchedObject.elemList);
    this.setState({
      searchedObject: searchedObject
    }, () => this.render());

  }

  render(){
    var availableFeatures = new AvailableFeatures();
    var rowIndex = -1;
    var searchedFeatures;
    searchedFeatures = <div><table>
      {this.state.searchedFeatures.map((elem) => {
        rowIndex++;
        return (
          <tr>
          <td>
            <select onChange = {this.handleFeatureTagSelect}>
            <option value={[rowIndex, 0, "ignore"]}>---Please select type---</option>
            {Object.entries(availableFeatures.features).map(([key, value]) => {
              return (<option value={[rowIndex, 0, key]} class="boldOption">{key}</option>)
              }
            )}
            </select>
          </td>
            {this.renderAdditionalFields(rowIndex, 1, this.state.searchedFeatures[rowIndex].elemList[0], this.handleFeatureTagSelect)}
            {this.renderAdditionalFields(rowIndex, 2, [this.state.searchedFeatures[rowIndex].elemList[0], this.state.searchedFeatures[rowIndex].elemList[1]], this.handleFeatureTagSelect)}
          <td><label>Distance: </label><input onChange = {this.handleDistanceForSearchedFeaturesChange} name={rowIndex} value={this.state.searchedFeatures[rowIndex].distance}></input></td>
          <td><button onClick = {this.removeSearchedFeature} value={rowIndex}>Remove</button></td>
          </tr>)}
      )}
    </table>
    <button onClick = {this.addNewRowForSearchedFeature}>Add</button>
    <br></br>
    <label>Koniunkcja? </label>
    <input type="checkbox"></input>
    </div>;

    var searchedObject = <div>
      <table>
        <tr>
          <td>
            <select onChange = {this.handleSearchedObjectTagSelect}>
            <option value={[rowIndex, 0, "ignore"]}>---Please select type---</option>
            {Object.entries(availableFeatures.features).map(([key, value]) => {
              return (<option value={[0, 0, key]} class="boldOption">{key}</option>)
              }
            )}
            </select>
          </td>
            {this.renderAdditionalFields(rowIndex, 1, this.state.searchedObject.elemList[0], this.handleSearchedObjectTagSelect)}
            {this.renderAdditionalFields(rowIndex, 2, [this.state.searchedObject.elemList[0], this.state.searchedObject.elemList[1]], this.handleSearchedObjectTagSelect)}
        </tr>
      </table>
    </div>

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
          {searchedObject}
          </table>
        </div>
        <div class = "searchedFeatures">
          <table align="center">
          {searchedFeatures}
          </table>
        </div>
        <div>
          <label>Vehicle: </label>
          <select>
            <option>Car</option>
            <option>Bike</option>
            <option>Walk</option>
          </select>
          <br></br>
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
      "water": ["lake", "rivier", "pond"],
      "hill": ["big", "medium", "small"]
    }, "beach", "wood"],
    "tourism": ["hotel", "guest_house", "camp_site"]
  }
}
class TagList{
  elemList = [null, null, null];
  distance = 0;
  //0 - cat
  //1 - subcat/tag
  //2 - tag for subcat
}

export default App;
