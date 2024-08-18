import {ComponentFixture, TestBed} from "@angular/core/testing";
import {ListQueryComponent} from "./list-query.component";
import {ListQueryService} from "./list-query.service";
import {MonacoEditorComponent} from "./monaco-editor/monaco-editor.component";
import {MockComponent} from "ng-mocks";

describe("ListQueryComponent", () => {
  let component: ListQueryComponent;
  let fixture: ComponentFixture<ListQueryComponent>;
  let mockListQueryService: { transformFilterToQuery: any, executeQuery: any }

  beforeEach(() => {
    mockListQueryService = {
      transformFilterToQuery: jasmine.createSpy("transformFilterToQuery"),
      executeQuery: jasmine.createSpy("executeQuery")
    };

    TestBed.configureTestingModule({
      declarations: [
        ListQueryComponent,
        MockComponent(MonacoEditorComponent)
      ],
      providers: [
        {provide: ListQueryService, useValue: mockListQueryService},
      ]
    })

    fixture = TestBed.createComponent(ListQueryComponent);
    component = fixture.componentInstance;

    component.filter = {};
    component.selectedInstances = 0;
    component.rows = [];
    component.columns = [];
  });

  it("should create the component", () => {
    expect(component).toBeTruthy();
  });

  it("should update query on filter change", () => {
    mockListQueryService.transformFilterToQuery.and.returnValue("SELECT * FROM data");
    component.filter = {name: "filter"}
    component.ngOnChanges({
      filter: {
        currentValue: {name: "filter"},
        previousValue: {},
        firstChange: true,
        isFirstChange: () => true
      }
    });

    expect(mockListQueryService.transformFilterToQuery).toHaveBeenCalledWith({name: "filter"});
    expect(component.query).toBe("SELECT * FROM data");
  });

  it("should emit redirectToComparison when onRedirectToComparison is called", () => {
    spyOn(component.redirectToComparison, "emit");

    component.onRedirectToComparison();

    expect(component.redirectToComparison.emit).toHaveBeenCalled();
  });

  it("should execute query and emit queryResult", async () => {
    const mockResponse = {rows: [{id: 1}], columns: ["id"]};
    component.editor = {query: "SELECT * FROM data"} as MonacoEditorComponent;
    mockListQueryService.executeQuery.and.returnValue(Promise.resolve(mockResponse));
    spyOn(component.queryResult, "emit");

    await component.executeQuery();

    expect(mockListQueryService.executeQuery).toHaveBeenCalledWith("SELECT * FROM data");
    expect(component.queryResult.emit).toHaveBeenCalledWith(mockResponse);
    expect(component.error).toBeUndefined();
  });

  it("should handle query execution error", async () => {
    const mockError = {error: ["Syntax error in query"]};
    component.editor = {query: "SELECT * FROM data"} as MonacoEditorComponent;
    mockListQueryService.executeQuery.and.returnValue(Promise.resolve(mockError));

    await component.executeQuery();

    expect(component.error).toEqual(["Syntax error in query"]);
  });

  it("should hide row when row has hidden property set to true and columns does not contain 'hidden'", () => {
    const row = {id: 1, hidden: true};
    const columns = ["Name",];

    expect(component.isRowHidden(row, columns)).toBeTrue();
  });

  it("should not hide row when row does not have hidden property", () => {
    const row = {id: 1};
    const columns = ["Name"];

    expect(component.isRowHidden(row, columns)).toBeFalse();
  });

  it("should not hide row when hidden property is not set to true", () => {
    const row = {id: 1, hidden: false};
    const columns = ["Name"];

    expect(component.isRowHidden(row, columns)).toBeFalse();
  });

  it("should not hide row when columns contain 'hidden'", () => {
    const row = {id: 1, hidden: true};
    const columns = ["Name", "hidden"];

    expect(component.isRowHidden(row, columns)).toBeFalse();
  });

  it("should detect empty rows when rows are empty", () => {
    component.rows = [];
    expect(component.isRowsEmpty()).toBeTrue();
  });

  it("should detect empty rows when all rows are hidden", () => {
    component.rows = [
      {id: 1, hidden: true},
      {id: 2, hidden: true}
    ];
    component.columns = ["id"]
    expect(component.isRowsEmpty()).toBeTrue();
  });

  it("should not detect empty rows when at least one row is visible", () => {
    component.rows = [
      {id: 1, hidden: true},
      {id: 2, hidden: false}
    ];
    component.columns = ["id"]
    expect(component.isRowsEmpty()).toBeFalse();
  });

  it("should not detect empty rows when rows are not empty and hidden in columns", () => {
    component.rows = [
      {id: 1, hidden: true},
      {id: 2, hidden: true}
    ];
    component.columns = ["id", "hidden"]
    expect(component.isRowsEmpty()).toBeFalse();
  });

  it("should download CSV without hidden rows when downloadCsv is called", () => {
    spyOn(document, "createElement").and.callFake(() => {
      return {click: jasmine.createSpy(), remove: jasmine.createSpy()} as any;
    });

    const mockRows = [{id: 1, name: "t2.micro", hidden: false}, {id: 2, name: "t3.micro", hidden: true}];
    const mockColumns = ["id", "name"];
    component.rows = mockRows;
    component.columns = mockColumns;

    component.downloadCsv();

    const csvStr = component["jsonToCsv"](mockRows, mockColumns);
    expect(csvStr).toBe("id;name\n1;t2.micro\n");
  });
});
