import {TestBed} from "@angular/core/testing";
import {RouterModule} from "@angular/router";
import {AppComponent} from "./app.component";
import {NavbarComponent} from "./navbar/navbar.component";
import {MockComponent} from "ng-mocks";

describe("AppComponent", () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterModule.forRoot([])
      ],
      declarations: [
        AppComponent,
        MockComponent(NavbarComponent)
      ],
    });
  });

  it("should create the app", () => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });
});
